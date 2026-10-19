package s10_transactions.tx01_transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test sekcji 3 lekcji Tx01Transactional — reguła wycofania dla CAŁEJ aplikacji (Spring Framework 6.2+).
 *
 * <p>Osobna klasa testowa, bo potrzebuje INNEJ konfiguracji niż Tx01TransactionalTest: do klasy startowej lekcji
 * dokładamy własną konfigurację z {@code @EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)}
 * (rollback on all exceptions = wycofuj przy wszystkich wyjątkach). Klasa konfiguracji jest zagnieżdżona w klasie
 * testu, więc skanowanie pakietu w innych testach jej NIE znajdzie (Spring Boot pomija klasy testowe przy skanowaniu
 * w testach) — tylko ten test ją widzi, bo podajemy ją wprost w {@code classes}.</p>
 */
@SpringBootTest(classes = {Tx01Application.class, Tx01TransactionalRollbackOnTest.AllExceptionsRollBack.class})
@ActiveProfiles("tx01")
class Tx01TransactionalRollbackOnTest {

    /** Własna konfiguracja transakcji: wycofuj także przy wyjątkach KONTROLOWANYCH. */
    @Configuration
    @EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
    static class AllExceptionsRollBack {
    }

    static final String BOOT_TRANSACTION_CONFIG = "org.springframework.boot.transaction.autoconfigure."
            + "TransactionAutoConfiguration$EnableTransactionManagementConfiguration$CglibAutoProxyConfiguration";

    @Autowired
    TransferService service;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void resetAccounts() {
        jdbc.sql("delete from accounts").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-JAN', 'Jan Kowalski', 100000)").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-MARIA', 'Maria Nowak', 50000)").update();
    }

    long balance(String number) {
        return jdbc.sql("select balance from accounts where number = ?").param(number).query(Long.class).single();
    }

    @Test
    @DisplayName("3. rollbackOn = ALL_EXCEPTIONS: wyjątek kontrolowany wycofuje transakcję bez rollbackFor")
    void allExceptionsRollBack() {
        assertThatThrownBy(() -> service.transferAndConfirm("PL-JAN", "PL-MARIA", 10000, false))
                .isInstanceOf(PaymentGatewayException.class);

        assertThat(balance("PL-JAN")).as("ta sama metoda co w Tx01TransactionalTest — tym razem WYCOFANA")
                .isEqualTo(100000);
        assertThat(balance("PL-MARIA")).isEqualTo(50000);

        // Boot widzi Twoją konfigurację transakcji i swojej domyślnej (z proxy CGLIB) już nie tworzy.
        assertThat(context.containsBean(BOOT_TRANSACTION_CONFIG)).isFalse();
    }
}
