package s10_transactions.tx04_read_only_isolation;

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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import s10_transactions.SqlCapture;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx04ReadOnlyIsolation na PEŁNEJ aplikacji, BEZ transakcji testu (dlaczego — s10_transactions/Tx01Transactional,
 * sekcja 5). Dane przywraca {@code @BeforeEach}. Współbieżność: druga transakcja („drugi użytkownik”, OtherUser
 * z REQUIRES_NEW) jest uruchamiana RĘCZNIE w środku pierwszej (parametr {@code meanwhile}) — zawsze w tej samej
 * kolejności, bez wątków i sleepów. SQL podsłuchuje SqlCapture (StatementInspector Hibernate).
 */
@SpringBootTest(classes = Tx04Application.class, properties = SqlCapture.PROPERTY)
@ActiveProfiles("tx04")
@ExtendWith(OutputCaptureExtension.class) // przechwytywanie konsoli — do sprawdzenia logów menedżera transakcji
class Tx04ReadOnlyIsolationTest {

    private static final String TX_LOGGER = "org.springframework.orm.jpa.JpaTransactionManager";
    private static final BigDecimal FIFTY = new BigDecimal("50.00");
    private static final BigDecimal THIRTY = new BigDecimal("30.00");

    @Autowired
    private ReadOnlyService readOnly;
    @Autowired
    private IsolationShowcase isolation;
    @Autowired
    private TopUpService topUps;
    @Autowired
    private LockingService locking;
    @Autowired
    private WalletWriter writer;
    @Autowired
    private OtherUser otherUser;
    @Autowired
    private WalletRepository wallets;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private LoggingSystem loggingSystem;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx04TestData.reset(jdbc);
        SqlCapture.clear();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: readOnly = true — FlushMode MANUAL, sesja i encja read-only, połączenie read-only; zmiana nie trafia do bazy")
    void section1ReadOnlyDoesNotSaveEntityChanges() {
        ReadOnlyService.ReadOnlyView view = readOnly.renameInReadOnly(1, "Ewa Lis");

        assertThat(view).isEqualTo(new ReadOnlyService.ReadOnlyView("Ewa Lis", true, "MANUAL", true, true, true));
        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Anna Nowak");
        assertThat(SqlCapture.startingWith("update")).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: nawet jawny em.flush() w transakcji read-only nie zapisuje zmienionej encji")
    void section1ExplicitFlushDoesNothing() {
        readOnly.renameAndFlushInReadOnly(1, "Ewa Lis");

        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Anna Nowak");
        assertThat(SqlCapture.startingWith("update")).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: PUŁAPKA — jawny UPDATE przez JdbcClient w transakcji read-only w H2 przechodzi i jest zatwierdzony")
    void section1ReadOnlyIsNotAProtection() {
        assertThat(readOnly.renameWithJdbcInReadOnly(1, "Ewa Lis")).isEqualTo(1);

        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Ewa Lis");
    }

    @Test
    @DisplayName("Sekcja 1: findById z Spring Data otwiera własną transakcję readOnly (log JpaTransactionManager)")
    void section1SpringDataFindByIdIsReadOnly(CapturedOutput output) {
        loggingSystem.setLogLevel(TX_LOGGER, LogLevel.DEBUG);
        try {
            wallets.findById(1L);
        } finally {
            loggingSystem.setLogLevel(TX_LOGGER, null); // null = wróć do poziomu z konfiguracji
        }

        assertThat(output).contains("Creating new transaction with name "
                + "[org.springframework.data.jpa.repository.support.SimpleJpaRepository.findById]: "
                + "PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: domyślna izolacja H2 to READ COMMITTED; isolation = REPEATABLE_READ przestawia połączenie")
    void section2DefaultIsolation() {
        assertThat(isolation.defaultIsolation()).isEqualTo("READ COMMITTED");
        assertThat(isolation.repeatableReadIsolation()).isEqualTo("REPEATABLE READ");
    }

    @Test
    @DisplayName("Sekcja 2: READ COMMITTED — non-repeatable read: drugi odczyt widzi cudzy commit (100.00, potem 150.00)")
    void section2NonRepeatableRead() {
        assertThat(isolation.readBalanceTwice(1, () -> otherUser.topUp(1, FIFTY)))
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Sekcja 2: REPEATABLE READ — oba odczyty dają 100.00, choć w bazie jest już 150.00")
    void section2RepeatableRead() {
        assertThat(isolation.readBalanceTwiceRepeatable(1, () -> otherUser.topUp(1, FIFTY)))
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — JPA zwraca encję z kontekstu utrwalania: „powtarzalny”, ale nieaktualny odczyt")
    void section2JpaFirstLevelCacheHidesChange() {
        assertThat(isolation.readBalanceTwiceWithJpa(1, () -> otherUser.topUp(1, FIFTY)))
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Sekcja 2: dirty read — READ_UNCOMMITTED widzi niezatwierdzone 999.00, READ_COMMITTED widzi 100.00")
    void section2DirtyRead() {
        IsolationShowcase.DirtyReadView view = isolation.dirtyRead(1);

        assertThat(view.seenWithReadCommitted()).isEqualByComparingTo("100.00");
        assertThat(view.seenWithReadUncommitted()).isEqualByComparingTo("999.00");
        assertThat(Tx04TestData.balance(jdbc, 1)).as("zmiana wycofana").isEqualByComparingTo("100.00");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: lost update — bez wersji druga transakcja nadpisuje doładowanie: 130.00 zamiast 180.00")
    void section3LostUpdate() {
        assertThat(topUps.topUp(1, THIRTY, () -> otherUser.topUp(1, FIFTY))).isEqualByComparingTo("130.00");

        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("130.00");
        assertThat(SqlCapture.startingWith("update"))
                .containsExactly("update wallet set balance=?,owner=? where id=?",
                        "update wallet set balance=?,owner=? where id=?");
    }

    @Test
    @DisplayName("Sekcja 3: @Version — UPDATE z warunkiem na wersję trafia w 0 wierszy → ObjectOptimisticLockingFailureException")
    void section3OptimisticLocking() {
        assertThatThrownBy(() -> topUps.topUpVersioned(1, THIRTY, () -> otherUser.topUpVersioned(1, FIFTY)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                .hasMessageStartingWith("Unexpected row count (expected row count 1 but was 0) "
                        + "[update versioned_wallet set balance=?,owner=?,version=? where id=? and version=?]")
                .hasMessageContaining("VersionedWallet with id '1'")
                .hasRootCauseInstanceOf(org.hibernate.StaleStateException.class);

        assertThat(Tx04TestData.versionedBalance(jdbc, 1)).as("zostaje zmiana B").isEqualByComparingTo("150.00");
        assertThat(Tx04TestData.version(jdbc, 1)).isEqualTo(1);
    }

    @Test
    @DisplayName("Sekcja 3: dla porównania REPEATABLE READ bez wersji — H2 sama wykrywa konflikt zapisu")
    void section3RepeatableReadDetectsConflictInH2() {
        assertThatThrownBy(() -> topUps.topUpRepeatableRead(1, THIRTY, () -> otherUser.topUp(1, FIFTY)))
                .isInstanceOf(CannotAcquireLockException.class)
                .hasMessageContaining("Deadlock detected. The current transaction was rolled back.");

        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("150.00");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: @Lock(PESSIMISTIC_WRITE) → select … for update; z lock.timeout = 0 → for update nowait")
    void section4PessimisticLockSql() {
        tx.executeWithoutResult(status -> wallets.findForUpdateById(1L));
        tx.executeWithoutResult(status -> wallets.findForUpdateNoWaitById(1L));

        assertThat(SqlCapture.all()).containsExactly(
                "select w1_0.id,w1_0.balance,w1_0.owner from wallet w1_0 where w1_0.id=? for update",
                "select w1_0.id,w1_0.balance,w1_0.owner from wallet w1_0 where w1_0.id=? for update nowait");
    }

    @Test
    @DisplayName("Sekcja 4: wiersz zablokowany przez A — B z nowait dostaje CannotAcquireLockException od razu; A też wycofana")
    void section4SecondLockerFailsImmediately() {
        assertThatThrownBy(() -> locking.withdrawWithLock(1, THIRTY, () -> otherUser.topUpWithLockNoWait(1, FIFTY)))
                .isInstanceOf(CannotAcquireLockException.class)
                .hasMessageContaining("Timeout trying to lock table \"WALLET\"");

        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Sekcja 4: bez konkurencji wypłata z blokadą działa normalnie (100.00 − 30.00 = 70.00)")
    void section4LockedWithdrawal() {
        assertThat(locking.withdrawWithLock(1, THIRTY, () -> { })).isEqualByComparingTo("70.00");

        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — metoda z @Lock wywołana bez transakcji → InvalidDataAccessApiUsageException")
    void section4LockWithoutTransaction() {
        assertThatThrownBy(() -> wallets.findForUpdateById(1L))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("No active transaction")
                .hasRootCauseInstanceOf(jakarta.persistence.TransactionRequiredException.class);
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: izolacja metody działa, gdy metoda OTWIERA transakcję; przy dołączeniu (REQUIRED) jest ignorowana")
    void section5IsolationIgnoredWhenJoining() {
        assertThat(writer.isolationSeenBySerializableMethod()).isEqualTo("SERIALIZABLE");
        assertThat(isolation.isolationSeenByJoiningSerializableMethod()).isEqualTo("READ COMMITTED");
    }

    @Test
    @DisplayName("Sekcja 5: PUŁAPKA — metoda pisząca wywołana z transakcji read-only dołącza do niej; zmiana przepada bez błędu")
    void section5WriterJoinsReadOnlyTransaction() {
        readOnly.renameThroughWriter(1, "Ewa Lis");
        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Anna Nowak");

        writer.rename(1, "Ewa Lis"); // wywołana sama — własna, zwykła transakcja
        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Ewa Lis");
    }

    @Test
    @DisplayName("Sekcja 5: timeout = 5 trafia do transakcji Hibernate (Transaction.getTimeout() = 5 sekund)")
    void section5TimeoutPassedToHibernate() {
        assertThat(writer.hibernateTimeoutSeconds()).isEqualTo(5);
    }
}
