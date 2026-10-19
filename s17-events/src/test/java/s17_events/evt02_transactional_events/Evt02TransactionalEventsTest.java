package s17_events.evt02_transactional_events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Evt02TransactionalEvents. Używamy {@code @SpringBootTest}, bo potrzebujemy prawdziwej bazy H2
 * i prawdziwego menedżera transakcji. Testy NAPRAWDĘ zatwierdzają dane (inaczej nie widać różnicy między commitem
 * a rollbackiem), więc {@code @BeforeEach} czyści tabele i dziennik — kolejność testów nie ma znaczenia.
 */
@SpringBootTest
@ActiveProfiles("evt02")
@ExtendWith(OutputCaptureExtension.class)
class Evt02TransactionalEventsTest {

    private static final BigDecimal TOTAL = new BigDecimal("349.90");

    @Autowired
    private OrderService service;
    @Autowired
    private OrderRepository repository;
    @Autowired
    private EventLog log;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        log.clear();
    }


    @Test
    @DisplayName("1. Po zatwierdzeniu: zwykły słuchacz od razu, potem BEFORE_COMMIT, AFTER_COMMIT i AFTER_COMPLETION")
    void section1() {
        service.placeOrder("ZAM-001", "Jan Kowalski", TOTAL);

        assertThat(repository.countOrders()).isEqualTo(1);
        assertThat(log.whats()).containsExactly("zwykły(transakcja aktywna=true)", "serwis:koniec-metody",
                "BEFORE_COMMIT", "AFTER_COMMIT(domyślna)", "AFTER_COMMIT+fallbackExecution", "AFTER_COMPLETION");
    }

    @Test
    @DisplayName("2. Po wycofaniu: AFTER_COMMIT się nie wykonuje, AFTER_ROLLBACK tak, a zwykły słuchacz zdążył zareagować")
    void section2() {
        assertThatThrownBy(() -> service.placeOrderAndFail("ZAM-002", "Maria Nowak", new BigDecimal("64.99")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Płatność odrzucona");

        assertThat(repository.countOrders()).isZero();   // transakcja wycofana — zamówienia nie ma w bazie
        assertThat(log.whats()).containsExactly("zwykły(transakcja aktywna=true)", "AFTER_ROLLBACK", "AFTER_COMPLETION");
        // „zwykły” słuchacz zareagował na zamówienie, którego nie ma — tak działałby e-mail „dziękujemy za zamówienie”.
    }

    @Test
    @DisplayName("3. Publikacja bez transakcji: AFTER_COMMIT jest pomijany, chyba że fallbackExecution = true")
    void section3() {
        service.publishWithoutTransaction("ZAM-003", "Adam Mazur", TOTAL);

        assertThat(log.whats()).containsExactly("zwykły(transakcja aktywna=false)", "AFTER_COMMIT+fallbackExecution");
    }

    @Test
    @DisplayName("4a. Wyjątek w BEFORE_COMMIT dociera do wywołującego i wycofuje transakcję")
    void section4a() {
        assertThatThrownBy(() -> service.placeOrderWithAudit("ZAM-004", "Zofia Krawczyk", TOTAL, "BEFORE_COMMIT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Audyt odrzucił zamówienie ZAM-004");

        assertThat(repository.countOrders()).isZero();
        assertThat(log.whats()).containsExactly("audyt:BEFORE_COMMIT");   // AFTER_COMMIT nie wykonał się
    }

    @Test
    @DisplayName("4b. Wyjątek w AFTER_COMMIT NIE dociera do wywołującego (tylko log ERROR), a zamówienie zostaje zapisane")
    void section4b(CapturedOutput output) {
        assertThatCode(() -> service.placeOrderWithAudit("ZAM-005", "Ola Pawlak", TOTAL, "AFTER_COMMIT"))
                .doesNotThrowAnyException();

        assertThat(repository.countOrders()).isEqualTo(1);
        assertThat(output.getOut()).contains("TransactionSynchronization.afterCompletion threw exception")
                .contains("Audyt po commicie zawiódł dla ZAM-005");
    }

    @Test
    @DisplayName("5. Naiwny zapis w AFTER_COMMIT: „transakcja aktywna”, autoCommit=false — a błąd po zapisie niczego nie cofa")
    void section5() {
        service.placeOrderNaivePoints("ZAM-006", "Jan Kowalski", TOTAL, false);
        assertThat(log.whats()).containsExactly("naiwny: transakcja aktywna=true, autoCommit=false",
                "naiwny: inna sesja widzi zapis=false");   // jeszcze niezatwierdzony
        assertThat(repository.countPoints()).isEqualTo(1);   // w tej konfiguracji (JDBC + Hikari + H2) wiersz jednak jest

        repository.deleteAll();
        assertThatCode(() -> service.placeOrderNaivePoints("ZAM-007", "Jan Kowalski", TOTAL, true))
                .doesNotThrowAnyException();   // wyjątek z AFTER_COMMIT nie wraca do wywołującego
        assertThat(repository.countPoints()).isEqualTo(1);   // zapis zostaje mimo wyjątku — nikt go nie wycofał
    }

    @Test
    @DisplayName("6a. Słuchacz z REQUIRES_NEW zapisuje w osobnej transakcji: sukces zatwierdza, błąd wycofuje tylko jego zapis")
    void section6a() {
        service.placeOrderCorrectPoints("ZAM-008", "Jan Kowalski", TOTAL, false);
        assertThat(repository.countPoints()).isEqualTo(1);

        repository.deleteAll();
        assertThatCode(() -> service.placeOrderCorrectPoints("ZAM-009", "Jan Kowalski", TOTAL, true))
                .doesNotThrowAnyException();
        assertThat(repository.countOrders()).isEqualTo(1);   // zamówienie (już zatwierdzone) zostaje
        assertThat(repository.countPoints()).isZero();       // punkty wycofane — osobna transakcja zadziałała
    }

    /** Konfiguracja pomocnicza do sekcji 6b (celowo bez @Configuration — inaczej @SpringBootTest wziąłby ją za konfigurację testu). */
    @EnableTransactionManagement
    static class PlainTransactionalListenerConfig {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        BadListener badListener() {
            return new BadListener();
        }
    }

    static class BadListener {
        @TransactionalEventListener
        @Transactional
        void on(String event) {
        }
    }

    @Test
    @DisplayName("6b. @Transactional bez REQUIRES_NEW na słuchaczu @TransactionalEventListener: Spring odrzuca go już przy starcie")
    void section6b() {
        assertThatThrownBy(() -> new AnnotationConfigApplicationContext(PlainTransactionalListenerConfig.class))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessageStartingWith("@TransactionalEventListener method must not be annotated with @Transactional "
                        + "unless when declared as REQUIRES_NEW or NOT_SUPPORTED");
    }
}
