package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Sekcja 3 lekcji Tx05ProgrammaticTransactions: {@code PlatformTransactionManager} (menedżer transakcji platformy)
 * użyty WPROST — {@code getTransaction} / {@code commit} / {@code rollback}. Dokładnie to (z dodatkami) robią za nas
 * proxy {@code @Transactional} (TransactionInterceptor) i {@code TransactionTemplate.execute}.
 */
@Service
public class ManualOrderService {

    private final PlatformTransactionManager transactionManager; // bean "transactionManager" = JpaTransactionManager
    private final JdbcClient jdbc;

    public ManualOrderService(PlatformTransactionManager transactionManager, JdbcClient jdbc) {
        this.transactionManager = transactionManager;
        this.jdbc = jdbc;
    }

    /** Zamówienie + wpis audytu w jednej transakcji, obsługiwanej ręcznie. */
    public void placeOrder(OrderLine line) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition(); // REQUIRED, DEFAULT, nie readOnly
        definition.setName("manualPlaceOrder");

        TransactionStatus status = transactionManager.getTransaction(definition); // begin (albo dołącz — wg propagacji)
        try {
            OrderSql.insertOrder(jdbc, line, "NEW");
            OrderSql.insertAudit(jdbc, "Przyjęto zamówienie " + line.number());
        } catch (RuntimeException | Error e) {
            transactionManager.rollback(status); // wycofaj i przekaż błąd dalej
            throw e;
        }
        transactionManager.commit(status); // POZA try: gdy sam commit się nie uda, nie wołamy już rollback
    }

    /** PUŁAPKA: drugi commit tej samej transakcji. */
    public void commitTwice(OrderLine line) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        OrderSql.insertOrder(jdbc, line, "NEW");
        transactionManager.commit(status);
        transactionManager.commit(status); // → IllegalTransactionStateException
    }
}
