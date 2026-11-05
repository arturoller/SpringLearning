package s10_transactions.tx05_programmatic_transactions;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx05ProgrammaticTransactions na PEŁNEJ aplikacji, BEZ transakcji testu (dlaczego —
 * s10_transactions/Tx01Transactional, sekcja 5). Dane i atrapę poczty czyści {@code @BeforeEach}.
 */
@SpringBootTest(classes = Tx05Application.class)
@ActiveProfiles("tx05")
@ExtendWith(OutputCaptureExtension.class) // przechwytywanie konsoli — do sprawdzenia logów menedżera transakcji
class Tx05ProgrammaticTransactionsTest {

    private static final String TX_LOGGER = "org.springframework.orm.jpa.JpaTransactionManager";

    @Autowired
    private OrderImporter importer;
    @Autowired
    private TemplateSettingsShowcase settings;
    @Autowired
    private ManualOrderService manual;
    @Autowired
    private OrderNotifications notifications;
    @Autowired
    private FakeEmailSender mail;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private LoggingSystem loggingSystem;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx05TestData.reset(jdbc);
        mail.clear();
        notifications.clearCompletions();
    }

    private void withTransactionDebugLog(Runnable action) {
        loggingSystem.setLogLevel(TX_LOGGER, LogLevel.DEBUG);
        try {
            action.run();
        } finally {
            loggingSystem.setLogLevel(TX_LOGGER, null); // null = wróć do poziomu z konfiguracji
        }
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: Boot tworzy bean TransactionTemplate z ustawieniami domyślnymi (REQUIRED, DEFAULT) na JpaTransactionManager")
    void section1AutoConfiguredTemplate() {
        assertThat(tx.getTransactionManager()).isSameAs(transactionManager).isInstanceOf(JpaTransactionManager.class);
        assertThat(tx.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
        assertThat(tx.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
        assertThat(tx.isReadOnly()).isFalse();
        assertThat(tx.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
    }

    @Test
    @DisplayName("Sekcja 1: partia z osobną transakcją na element — złe pozycje odrzucone, dobre zapisane")
    void section1TransactionPerItem() {
        OrderImporter.ImportReport report = importer.importEach(List.of(OrderLine.of("ZAM-1", "10.00"),
                OrderLine.of("ZAM-2", "-1.00"), OrderLine.of("ZAM-1", "5.00"), OrderLine.of("ZAM-3", "7.00")));

        assertThat(report.imported()).containsExactly("ZAM-1", "ZAM-3");
        assertThat(report.failed()).as("kwota ujemna (CHECK) i duplikat numeru (UNIQUE)").containsExactly("ZAM-2", "ZAM-1");
        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1", "ZAM-3");
    }

    @Test
    @DisplayName("Sekcja 1: dla porównania jedna transakcja na partię — jedna zła pozycja wycofuje wszystkie")
    void section1OneTransactionForAll() {
        assertThatThrownBy(() -> importer.importAllOrNothing(List.of(OrderLine.of("ZAM-1", "10.00"),
                OrderLine.of("ZAM-2", "-1.00"))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("CK_CUSTOMER_ORDER_AMOUNT");

        assertThat(Tx05TestData.orders(jdbc)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: PUŁAPKA — @Transactional na metodzie z pętlą szablonów: wszystko w jednej transakcji, UnexpectedRollbackException")
    void section1TemplatesJoinOuterTransaction() {
        assertThatThrownBy(() -> importer.importEachInsideOneTransaction(List.of(OrderLine.of("ZAM-1", "10.00"),
                OrderLine.of("ZAM-2", "-1.00"), OrderLine.of("ZAM-3", "7.00"))))
                .isInstanceOf(UnexpectedRollbackException.class)
                .hasMessage("Transaction silently rolled back because it has been marked as rollback-only");

        assertThat(Tx05TestData.orders(jdbc)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: tylko część metody w transakcji — przygotowanie bez transakcji, zapis w transakcji")
    void section1PartOfMethod() {
        OrderImporter.PartialView view = importer.saveWithPreparation(OrderLine.of(" zam-7 ", "10.00"));

        assertThat(view.duringPreparation().active()).isFalse();
        assertThat(view.duringSave().active()).isTrue();
        assertThat(view.duringSave().name()).as("szablon bez setName — transakcja bez nazwy").isNull();
        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-7");
    }

    @Test
    @DisplayName("Sekcja 1: TransactionTemplate działa bez proxy — w obiekcie z new i w metodzie prywatnej (rollback obu zapisów)")
    void section1TemplateWithoutProxy() {
        PlainOrderWriter writer = new PlainOrderWriter(tx, jdbc);

        assertThatThrownBy(() -> writer.writePair(OrderLine.of("ZAM-1", "10.00"), OrderLine.of("ZAM-2", "0.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(Tx05TestData.orders(jdbc)).as("pierwszy zapis też wycofany").isEmpty();

        writer.writePair(OrderLine.of("ZAM-1", "10.00"), OrderLine.of("ZAM-2", "20.00"));
        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1", "ZAM-2");
    }

    @Test
    @DisplayName("Sekcja 1: wyjątek checked z lambdy → UndeclaredThrowableException i rollback")
    void section1CheckedExceptionFromCallback() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            jdbc.sql("INSERT INTO audit_log (message) VALUES ('próba')").update();
            throw sneaky(new Exception("plik importu nie istnieje"));
        }))
                .isInstanceOf(UndeclaredThrowableException.class)
                .hasMessage("TransactionCallback threw undeclared checked exception")
                .cause().hasMessage("plik importu nie istnieje");

        assertThat(Tx05TestData.auditMessages(jdbc)).isEmpty();
    }

    /**
     * Lambda {@code TransactionCallback} nie może zadeklarować {@code throws Exception}; „przemycamy” wyjątek checked
     * sztuczką z typem generycznym (sneaky throw = podstępne rzucenie) — tylko po to, żeby zobaczyć reakcję szablonu.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneaky(Throwable e) throws E {
        throw (E) e;
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: status.setRollbackOnly() — w środku zapis widoczny (1), po wyjściu wycofany; wynik lambdy wraca")
    void section2SetRollbackOnly() {
        assertThat(settings.dryRunInsert(OrderLine.of("ZAM-1", "10.00"))).isEqualTo(1);

        assertThat(Tx05TestData.orders(jdbc)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 2: własny szablon — nazwa, readOnly, REPEATABLE READ i timeout widać w transakcji, w bazie i w logu")
    void section2CustomTemplate(CapturedOutput output) {
        TemplateSettingsShowcase.ReportView[] view = new TemplateSettingsShowcase.ReportView[1];
        withTransactionDebugLog(() -> view[0] = settings.reportSettings());

        assertThat(view[0].state()).isEqualTo(new TxState(true, "monthlyReport", true));
        assertThat(view[0].isolationLevel()).isEqualTo("REPEATABLE READ");
        assertThat(output).contains("Creating new transaction with name [monthlyReport]: "
                + "PROPAGATION_REQUIRED,ISOLATION_REPEATABLE_READ,timeout_10,readOnly");
        assertThat(tx.isReadOnly()).as("wspólny bean bez zmian").isFalse();
    }

    @Test
    @DisplayName("Sekcja 2: isNewTransaction() — zewnętrzny tak, dołączony (REQUIRED) nie, REQUIRES_NEW tak i na innym połączeniu")
    void section2NewTransactionFlags() {
        assertThat(settings.nesting())
                .isEqualTo(new TemplateSettingsShowcase.NestingView(true, false, true, true));
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: PlatformTransactionManager wprost — commit zapisuje oba wiersze, rollback cofa oba")
    void section3ManualTransaction(CapturedOutput output) {
        withTransactionDebugLog(() -> manual.placeOrder(OrderLine.of("ZAM-1", "10.00")));
        assertThatThrownBy(() -> manual.placeOrder(OrderLine.of("ZAM-2", "0.00")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1");
        assertThat(Tx05TestData.auditMessages(jdbc)).containsExactly("Przyjęto zamówienie ZAM-1");
        assertThat(output).contains("Creating new transaction with name [manualPlaceOrder]: "
                        + "PROPAGATION_REQUIRED,ISOLATION_DEFAULT")
                .contains("Initiating transaction commit");
    }

    @Test
    @DisplayName("Sekcja 3: PUŁAPKA — drugi commit tej samej transakcji → IllegalTransactionStateException")
    void section3CommitTwice() {
        assertThatThrownBy(() -> manual.commitTwice(OrderLine.of("ZAM-1", "10.00")))
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessage("Transaction is already completed - do not call commit or rollback more than once per transaction");

        assertThat(Tx05TestData.orders(jdbc)).as("pierwszy commit się udał").containsExactly("ZAM-1");
    }

    @Test
    @DisplayName("Sekcja 3: nieudany commit też kończy transakcję — rollback po nim rzuca IllegalTransactionStateException")
    void section3FailedCommitCompletesTransaction() {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        // uczestnik (szablon REQUIRED w środku) zawodzi → cała transakcja oznaczona jako rollback-only
        assertThatThrownBy(() -> tx.executeWithoutResult(inner -> {
            throw new IllegalStateException("błąd uczestnika");
        })).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> transactionManager.commit(status)).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(status.isCompleted()).isTrue();
        assertThatThrownBy(() -> transactionManager.rollback(status))
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessageStartingWith("Transaction is already completed");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: PRZED — e-mail wysłany w środku transakcji wychodzi, choć zamówienie zostało wycofane")
    void section4MailInsideTransaction() {
        assertThatThrownBy(() -> notifications.placeOrderSendingMailImmediately(OrderLine.of("ZAM-1", "10.00"), true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(Tx05TestData.orders(jdbc)).isEmpty();
        assertThat(mail.sent()).containsExactly("klient@example.com: Przyjęliśmy zamówienie ZAM-1");
    }

    @Test
    @DisplayName("Sekcja 4: PO — afterCommit wysyła e-mail tylko po commicie; afterCompletion dostaje COMMITTED / ROLLED_BACK")
    void section4MailAfterCommit() {
        notifications.placeOrderSendingMailAfterCommit(OrderLine.of("ZAM-1", "10.00"), false);
        assertThatThrownBy(() -> notifications.placeOrderSendingMailAfterCommit(OrderLine.of("ZAM-2", "10.00"), true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1");
        assertThat(mail.sent()).containsExactly("klient@example.com: Przyjęliśmy zamówienie ZAM-1");
        assertThat(notifications.completions()).containsExactly("ZAM-1: COMMITTED", "ZAM-2: ROLLED_BACK");
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — wyjątek w afterCommit dociera do wywołującego, choć zamówienie JEST zatwierdzone")
    void section4ExceptionInAfterCommit() {
        assertThatThrownBy(() -> notifications.placeOrderWithFailingMail(OrderLine.of("ZAM-1", "10.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Serwer poczty nie odpowiada");

        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1");
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — w afterCommit „transakcja” nadal aktywna, a połączenie już w auto-commit")
    void section4WriteInAfterCommit() {
        notifications.placeOrderAndAuditInAfterCommit(OrderLine.of("ZAM-1", "10.00"));

        OrderNotifications.AfterCommitView view = notifications.lastAfterCommitView();
        assertThat(view.state().active()).isTrue();
        assertThat(view.autoCommit()).isTrue();
        assertThat(Tx05TestData.auditMessages(jdbc)).as("zapisane od razu, poza transakcją")
                .containsExactly("Wysłano potwierdzenie ZAM-1");
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — registerSynchronization bez transakcji → IllegalStateException")
    void section4RegisterWithoutTransaction() {
        assertThatThrownBy(() -> notifications.registerWithoutTransaction())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Transaction synchronization is not active");
    }
}
