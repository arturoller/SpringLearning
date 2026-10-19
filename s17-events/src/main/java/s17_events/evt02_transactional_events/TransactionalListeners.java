package s17_events.evt02_transactional_events;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Słuchacze zdarzenia {@link OrderPlaced} w różnych fazach transakcji. Każdy zapisuje w dzienniku swoją nazwę
 * (a „zwykły” także to, czy transakcja jest aktywna). Liczby w {@code @Order} ustalają kolejność słuchaczy — bez nich
 * kolejność słuchaczy tego samego zdarzenia jest nieokreślona.
 */
@Component
public class TransactionalListeners {

    private final EventLog log;

    public TransactionalListeners(EventLog log) {
        this.log = log;
    }

    /** Zwykły słuchacz: reaguje od razu, w środku transakcji. */
    @Order(0)
    @EventListener
    public void immediately(OrderPlaced event) {
        log.add("zwykły(transakcja aktywna=" + TransactionSynchronizationManager.isActualTransactionActive() + ")");
    }

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void beforeCommit(OrderPlaced event) {
        log.add("BEFORE_COMMIT");
    }

    /** Faza domyślna ({@code phase} pominięte) to AFTER_COMMIT. */
    @Order(2)
    @TransactionalEventListener
    public void afterCommitDefault(OrderPlaced event) {
        log.add("AFTER_COMMIT(domyślna)");
    }

    @Order(4)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void afterRollback(OrderPlaced event) {
        log.add("AFTER_ROLLBACK");
    }

    @Order(5)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void afterCompletion(OrderPlaced event) {
        log.add("AFTER_COMPLETION");
    }

    /** {@code fallbackExecution = true}: wykonaj się także wtedy, gdy zdarzenie opublikowano BEZ transakcji. */
    @Order(3)
    @TransactionalEventListener(fallbackExecution = true)
    public void withFallback(OrderPlaced event) {
        log.add("AFTER_COMMIT+fallbackExecution");
    }
}
