package s10_transactions.solutions.tx05_programmatic_transactions;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import s10_transactions.tx05_programmatic_transactions.FakeEmailSender;
import s10_transactions.tx05_programmatic_transactions.OrderLine;
import s10_transactions.tx05_programmatic_transactions.ProgrammaticExercises.ReportSnapshot;
import s10_transactions.tx05_programmatic_transactions.TxState;

import java.util.ArrayList;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Tx05ProgrammaticTransactions (testy: Tx05ProgrammaticTransactionsSolutionsTest). */
@Service
public class SolutionProgrammaticExercises {

    private final TransactionTemplate tx;
    private final TransactionTemplate reportTx;
    private final JdbcClient jdbc;
    private final FakeEmailSender mail;

    public SolutionProgrammaticExercises(TransactionTemplate tx, PlatformTransactionManager transactionManager,
                                         JdbcClient jdbc, FakeEmailSender mail) {
        this.tx = tx;
        this.jdbc = jdbc;
        this.mail = mail;
        // ĆWICZENIE 3: osobna instancja szablonu na osobne ustawienia — tworzona raz
        this.reportTx = new TransactionTemplate(transactionManager);
        reportTx.setName("exerciseReport");
        reportTx.setReadOnly(true);
        reportTx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    /** ĆWICZENIE 1: setRollbackOnly — szablon zrobi rollback, a wynik lambdy i tak wróci. */
    public long dryRunCount(OrderLine line) {
        return tx.execute(status -> {
            insert(line);
            long count = jdbc.sql("SELECT COUNT(*) FROM customer_order").query(Long.class).single();
            status.setRollbackOnly();
            return count;
        });
    }

    /** ĆWICZENIE 2: transakcja na pozycję, błąd łapany POZA nią; metoda bez @Transactional. */
    public List<String> importReturningFailures(List<OrderLine> lines) {
        List<String> failed = new ArrayList<>();
        for (OrderLine line : lines) {
            try {
                tx.executeWithoutResult(status -> insert(line));
            } catch (DataAccessException e) {
                failed.add(line.number());
            }
        }
        return failed;
    }

    /** ĆWICZENIE 3. */
    public ReportSnapshot readOnlyReport() {
        return reportTx.execute(status -> new ReportSnapshot(TxState.now(),
                jdbc.sql("SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()")
                        .query(String.class).single()));
    }

    /** ĆWICZENIE 4: jedna transakcja; e-maile zarejestrowane jako afterCommit — po rollbacku nie wychodzą. */
    public void placeOrdersAndNotify(List<OrderLine> lines) {
        tx.executeWithoutResult(status -> {
            for (OrderLine line : lines) {
                insert(line);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mail.send("klient@example.com", "Przyjęliśmy zamówienie " + line.number());
                    }
                });
            }
        });
    }

    private void insert(OrderLine line) {
        jdbc.sql("INSERT INTO customer_order (order_number, amount, status) VALUES (?, ?, ?)")
                .param(line.number()).param(line.amount()).param("NEW").update();
    }
}
