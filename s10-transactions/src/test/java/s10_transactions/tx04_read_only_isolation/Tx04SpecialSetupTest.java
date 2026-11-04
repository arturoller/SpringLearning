package s10_transactions.tx04_read_only_isolation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import s10_transactions.tx04_pitfalls.validate_existing.ValidatingTransactionsConfig;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx04ReadOnlyIsolation, które potrzebują NIETYPOWYCH ustawień (krótki czas czekania na blokadę,
 * menedżer transakcji z validateExistingTransaction) — każdy uruchamia własną aplikację z własną bazą (UUID w nazwie),
 * żeby nie psuć innych testów. Czekanie jest ograniczone ustawieniem bazy ({@code LOCK_TIMEOUT=200} ms) — test nie
 * może się zawiesić.
 */
class Tx04SpecialSetupTest {

    private static ConfigurableApplicationContext start(String urlSuffix, Class<?>... extraSources) {
        Class<?>[] sources = new Class<?>[extraSources.length + 1];
        sources[0] = Tx04Application.class;
        System.arraycopy(extraSources, 0, sources, 1, extraSources.length);
        return new SpringApplicationBuilder(sources).web(WebApplicationType.NONE)
                .run("--spring.profiles.active=tx04",
                        "--spring.datasource.url=jdbc:h2:mem:tx04-" + UUID.randomUUID() + urlSuffix);
    }

    @Test
    @DisplayName("Sekcja 4: zwykły UPDATE czeka na blokadę FOR UPDATE innej transakcji — aż do limitu czasu blokady bazy")
    void section4UpdateWaitsForPessimisticLock() {
        try (ConfigurableApplicationContext app = start(";LOCK_TIMEOUT=200")) {
            LockingService locking = app.getBean(LockingService.class);
            OtherUser otherUser = app.getBean(OtherUser.class);

            // A trzyma blokadę wiersza; B (REQUIRES_NEW, drugie połączenie) próbuje zapisać ten sam wiersz i czeka
            assertThatThrownBy(() -> locking.withdrawWithLock(1, new BigDecimal("30.00"),
                    () -> otherUser.topUp(1, new BigDecimal("50.00"))))
                    .isInstanceOf(CannotAcquireLockException.class)
                    .hasMessageContaining("Timeout trying to lock table \"WALLET\"");
            assertThat(Tx04TestData.balance(app.getBean(JdbcClient.class), 1)).as("obie zmiany wycofane")
                    .isEqualByComparingTo("100.00");
        }
    }

    @Test
    @DisplayName("Sekcja 5: validateExistingTransaction = true — dołączenie z inną izolacją albo do read-only rzuca wyjątek")
    void section5ValidateExistingTransaction() {
        try (ConfigurableApplicationContext app = start("", ValidatingTransactionsConfig.class)) {
            assertThat(app.getBean(JpaTransactionManager.class).isValidateExistingTransaction()).isTrue();

            assertThatThrownBy(() -> app.getBean(IsolationShowcase.class).isolationSeenByJoiningSerializableMethod())
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessage("Participating transaction with definition [PROPAGATION_REQUIRED,ISOLATION_SERIALIZABLE] "
                            + "specifies isolation level which is incompatible with existing transaction: (unknown)");

            assertThatThrownBy(() -> app.getBean(ReadOnlyService.class).renameThroughWriter(1, "Ewa Lis"))
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessage("Participating transaction with definition [PROPAGATION_REQUIRED,ISOLATION_DEFAULT] "
                            + "is not marked as read-only but existing transaction is");
        }
    }
}
