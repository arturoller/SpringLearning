package s10_transactions.tx02_propagation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx02Propagation na PEŁNEJ aplikacji, BEZ transakcji testu (dlaczego — s10_transactions/Tx01Transactional,
 * sekcja 5). Dane przywraca {@code @BeforeEach}. Gdzie test potrzebuje transakcji „z zewnątrz”, otwiera ją
 * {@code TransactionTemplate} (szablon transakcji — bean z autokonfiguracji; s10_transactions/Tx05ProgrammaticTransactions).
 */
@SpringBootTest(classes = Tx02Application.class)
@ActiveProfiles("tx02")
@ExtendWith(OutputCaptureExtension.class) // przechwytywanie konsoli — do sprawdzenia logów menedżera transakcji
class Tx02PropagationTest {

    private static final String TX_LOGGER = "org.springframework.orm.jpa.JpaTransactionManager";

    @Autowired
    private OrderService orders;
    @Autowired
    private NestedOrderService nestedOrders;
    @Autowired
    private PropagationShowcase showcase;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx02TestData.reset(jdbc);
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: REQUIRED — rezerwacja dołącza do transakcji zamówienia; udane zamówienie zapisuje oba")
    void section1RequiredSuccess() {
        orders.placeOrder("ZAM-1", "Mysz", 2);

        assertThat(Tx02TestData.orders(jdbc)).containsExactly("ZAM-1:RESERVED");
        assertThat(Tx02TestData.stock(jdbc, "Mysz")).isZero();
    }

    @Test
    @DisplayName("Sekcja 1: REQUIRED — wyjątek w metodzie wewnętrznej wycofuje CAŁĄ transakcję (zamówienia nie ma)")
    void section1RequiredSharedFate() {
        assertThatThrownBy(() -> orders.placeOrder("ZAM-1", "Mysz", 3))
                .isInstanceOf(OutOfStockException.class)
                .hasMessage("Brak towaru: Mysz ×3");

        assertThat(Tx02TestData.orders(jdbc)).isEmpty();
        assertThat(Tx02TestData.stock(jdbc, "Mysz")).isEqualTo(2);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: złapany wyjątek z wewnętrznej REQUIRED → UnexpectedRollbackException przy commicie")
    void section2UnexpectedRollback() {
        assertThatThrownBy(() -> orders.placeOrderIgnoringStockError("ZAM-1", "Mysz", 3))
                .isInstanceOf(UnexpectedRollbackException.class)
                .hasMessage("Transaction silently rolled back because it has been marked as rollback-only");

        assertThat(Tx02TestData.orders(jdbc)).as("status WAITING też przepadł").isEmpty();
    }

    @Test
    @DisplayName("Sekcja 2: metoda wewnętrzna zwraca wynik zamiast wyjątku — zamówienie WAITING zostaje zapisane")
    void section2ReturnResultInsteadOfThrowing() {
        assertThat(orders.placeOrderOrWait("ZAM-1", "Mysz", 3)).isEqualTo("WAITING");
        assertThat(orders.placeOrderOrWait("ZAM-2", "Mysz", 2)).isEqualTo("RESERVED");

        assertThat(Tx02TestData.orders(jdbc)).containsExactly("ZAM-1:WAITING", "ZAM-2:RESERVED");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: REQUIRES_NEW — wpis audytu zostaje, choć transakcja zamówienia została wycofana")
    void section3RequiresNewSurvivesRollback() {
        assertThatThrownBy(() -> orders.placeOrder("ZAM-1", "Mysz", 3)).isInstanceOf(OutOfStockException.class);

        assertThat(Tx02TestData.orders(jdbc)).isEmpty();
        assertThat(Tx02TestData.auditMessages(jdbc)).containsExactly("Próba złożenia zamówienia ZAM-1");
    }

    @Test
    @DisplayName("Sekcja 3: REQUIRES_NEW ma DRUGIE połączenie i nie widzi niezatwierdzonego zamówienia transakcji zewnętrznej")
    void section3RequiresNewUsesSecondConnection() {
        OrderService.SuspendedView view = orders.insertOrderAndLookFromNewTransaction("ZAM-1");

        assertThat(view.inner().sessionId()).as("inna sesja H2 = inne połączenie").isNotEqualTo(view.outerSessionId());
        assertThat(view.inner().visibleOrders()).as("zamówienie jeszcze niezatwierdzone").isZero();
        assertThat(view.inner().state()).isEqualTo(
                new TxState(true, "s10_transactions.tx02_propagation.AuditLog.inspectFromNewTransaction"));
        assertThat(Tx02TestData.orders(jdbc)).as("po commicie zewnętrznej — jest").containsExactly("ZAM-1:NEW");
    }

    @Test
    @DisplayName("Sekcja 3: log JpaTransactionManager (DEBUG) — dołączanie (REQUIRED) i zawieszanie (REQUIRES_NEW)")
    void section3TransactionManagerLog(CapturedOutput output) {
        LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
        logging.setLogLevel(TX_LOGGER, LogLevel.DEBUG);
        try {
            assertThatThrownBy(() -> orders.placeOrder("ZAM-1", "Mysz", 3)).isInstanceOf(OutOfStockException.class);
        } finally {
            logging.setLogLevel(TX_LOGGER, null);
        }

        assertThat(output.getOut())
                .contains("Suspending current transaction, creating new transaction with name "
                        + "[s10_transactions.tx02_propagation.AuditLog.record]")
                .contains("Resuming suspended transaction after completion of inner transaction")
                .contains("Participating in existing transaction")
                .contains("Participating transaction failed - marking existing transaction as rollback-only");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER — bez transakcji")
    void section4WithoutTransaction() {
        assertThat(showcase.supports().active()).isFalse();
        assertThat(showcase.notSupported().active()).isFalse();
        assertThat(showcase.never().active()).isFalse();
        assertThatThrownBy(showcase::mandatory)
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessage("No existing transaction found for transaction marked with propagation 'mandatory'");
    }

    @Test
    @DisplayName("Sekcja 4: SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER — wewnątrz trwającej transakcji")
    void section4InsideTransaction() {
        TxState supports = tx.execute(status -> showcase.supports());
        TxState notSupported = tx.execute(status -> showcase.notSupported());
        TxState mandatory = tx.execute(status -> showcase.mandatory());

        assertThat(supports.active()).isTrue();
        assertThat(notSupported.active()).as("zewnętrzna zawieszona").isFalse();
        assertThat(mandatory.active()).isTrue();
        assertThatThrownBy(() -> tx.execute(status -> showcase.never()))
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessage("Existing transaction found for transaction marked with propagation 'never'");
    }

    @Test
    @DisplayName("Sekcja 4: NESTED z JpaTransactionManager → NestedTransactionNotSupportedException (domyślnie wyłączone)")
    void section4NestedNotSupportedByJpaManager() {
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);
        assertThat(((JpaTransactionManager) transactionManager).isNestedTransactionAllowed()).isFalse();

        assertThatThrownBy(() -> nestedOrders.placeOrderWithGiftUsingJpaManager("ZAM-1", "Mysz"))
                .isInstanceOf(NestedTransactionNotSupportedException.class)
                .hasMessage("Transaction manager does not allow nested transactions by default - "
                        + "specify 'nestedTransactionAllowed' property with value 'true'");
        assertThat(Tx02TestData.orders(jdbc)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 4: NESTED z JdbcTransactionManager = savepoint — brak prezentu wycofuje tylko rezerwację prezentu")
    void section4NestedWithSavepoint() {
        assertThat(nestedOrders.placeOrderWithOptionalGift("ZAM-1", "Klawiatura", "Mysz")).isTrue();
        assertThat(nestedOrders.placeOrderWithOptionalGift("ZAM-2", "Klawiatura", "Mysz")).isTrue();
        assertThat(nestedOrders.placeOrderWithOptionalGift("ZAM-3", "Klawiatura", "Mysz")).as("myszy już brak").isFalse();

        assertThat(Tx02TestData.orders(jdbc)).containsExactly("ZAM-1:RESERVED", "ZAM-2:RESERVED", "ZAM-3:RESERVED");
        assertThat(Tx02TestData.stock(jdbc, "Klawiatura")).isEqualTo(2);
        assertThat(Tx02TestData.stock(jdbc, "Mysz")).isZero();
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — dwa menedżery transakcji na jednym DataSource w jednym wywołaniu → CannotCreateTransactionException")
    void section4MixingTransactionManagersFails() {
        assertThatThrownBy(() -> nestedOrders.placeOrderMixingManagers("ZAM-1", "Klawiatura"))
                .isInstanceOf(CannotCreateTransactionException.class)
                .hasMessage("Could not open JPA EntityManager for transaction")
                .rootCause().isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Already value [org.springframework.jdbc.datasource.ConnectionHolder@")
                .hasMessageContaining("for key [HikariDataSource (HikariPool-") // numer puli zależy od kolejności testów
                .hasMessageEndingWith("bound to thread");
        assertThat(Tx02TestData.orders(jdbc)).isEmpty();
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: this.metodaZRequiresNew() omija proxy — audyt znika razem z zamówieniem")
    void section5SelfInvocationIgnoresRequiresNew() {
        assertThatThrownBy(() -> orders.placeOrderWithInternalAudit("ZAM-1", "Mysz", 3))
                .isInstanceOf(OutOfStockException.class);

        assertThat(Tx02TestData.auditMessages(jdbc)).as("REQUIRES_NEW nie zadziałało").isEmpty();
        assertThat(orders.transactionNameSeenBySelfInvokedRequiresNew())
                .isEqualTo("s10_transactions.tx02_propagation.OrderService.transactionNameSeenBySelfInvokedRequiresNew");
        assertThat(orders.currentTransactionNameInNewTransaction()).as("przez proxy — własna transakcja")
                .isEqualTo("s10_transactions.tx02_propagation.OrderService.currentTransactionNameInNewTransaction");
    }
}
