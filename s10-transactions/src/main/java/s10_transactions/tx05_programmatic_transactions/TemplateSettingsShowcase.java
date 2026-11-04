package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sekcja 2 lekcji Tx05ProgrammaticTransactions: {@code status.setRollbackOnly()} i szablony z WŁASNYMI ustawieniami.
 * Każdy zestaw ustawień = osobna instancja {@code TransactionTemplate} (tworzona raz, w konstruktorze) — wspólnego
 * beana {@code transactionTemplate} nie zmieniamy, bo korzysta z niego cała aplikacja.
 */
@Service
public class TemplateSettingsShowcase {

    private static final String SESSION_SQL = "SELECT SESSION_ID()";
    private static final String ISOLATION_SQL =
            "SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()";

    private final TransactionTemplate tx;            // wspólny bean z autokonfiguracji (REQUIRED, DEFAULT)
    private final TransactionTemplate reportTx;      // tylko do odczytu, REPEATABLE READ, 10 s, z nazwą
    private final TransactionTemplate requiresNewTx; // zawsze nowa transakcja
    private final JdbcClient jdbc;

    public TemplateSettingsShowcase(TransactionTemplate tx, PlatformTransactionManager transactionManager,
                                    JdbcClient jdbc) {
        this.tx = tx;
        this.jdbc = jdbc;

        this.reportTx = new TransactionTemplate(transactionManager);
        reportTx.setName("monthlyReport");                                        // nazwa (widać ją w logach)
        reportTx.setReadOnly(true);                                               // jak readOnly = true
        reportTx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ); // jak isolation = …
        reportTx.setTimeout(10);                                                  // jak timeout = 10 (sekundy)

        this.requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); // jak propagation = …
    }

    /**
     * „Próba na sucho” (dry run): zapis, odczyt z WNĘTRZA transakcji, a potem {@code status.setRollbackOnly()} —
     * szablon na końcu robi rollback zamiast commitu. Wynik lambdy i tak wraca do wywołującego.
     */
    public long dryRunInsert(OrderLine line) {
        return tx.execute(status -> {
            OrderSql.insertOrder(jdbc, line, "NEW");
            long visibleInside = OrderSql.countOrders(jdbc);
            status.setRollbackOnly(); // rollback-only = tylko do wycofania
            return visibleInside;
        });
    }

    /** Ustawienia szablonu raportowego widziane z wnętrza transakcji (Spring) i z bazy (poziom izolacji). */
    public ReportView reportSettings() {
        return reportTx.execute(status -> new ReportView(TxState.now(),
                jdbc.sql(ISOLATION_SQL).query(String.class).single()));
    }

    /**
     * {@code status.isNewTransaction()}: czy TEN szablon otworzył transakcję. Zewnętrzny szablon — tak; wspólny szablon
     * (REQUIRED) w środku — nie, dołączył; szablon REQUIRES_NEW w środku — tak, i ma inne połączenie (sesję H2).
     */
    public NestingView nesting() {
        return tx.execute(outer -> {
            int outerSession = jdbc.sql(SESSION_SQL).query(Integer.class).single();
            boolean joinedIsNew = tx.execute(joined -> joined.isNewTransaction());
            InnerView inner = requiresNewTx.execute(status -> new InnerView(status.isNewTransaction(),
                    jdbc.sql(SESSION_SQL).query(Integer.class).single()));
            return new NestingView(outer.isNewTransaction(), joinedIsNew, inner.isNew(),
                    inner.sessionId() != outerSession);
        });
    }

    private record InnerView(boolean isNew, int sessionId) {
    }

    /** Stan z wnętrza transakcji szablonu raportowego. */
    public record ReportView(TxState state, String isolationLevel) {
    }

    /** Kto otworzył transakcję i czy REQUIRES_NEW dostał inne połączenie. */
    public record NestingView(boolean outerIsNew, boolean joinedIsNew, boolean requiresNewIsNew,
                              boolean requiresNewHasOtherSession) {
    }
}
