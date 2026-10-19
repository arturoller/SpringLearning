package s17_events.evt02_transactional_events;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Słuchacze, którzy mogą rzucić wyjątek w fazie BEFORE_COMMIT albo AFTER_COMMIT — do porównania skutków. */
@Component
public class AuditListeners {

    private final EventLog log;

    public AuditListeners(EventLog log) {
        this.log = log;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void beforeCommit(AuditRequested event) {
        log.add("audyt:BEFORE_COMMIT");
        if ("BEFORE_COMMIT".equals(event.failIn())) {
            throw new IllegalStateException("Audyt odrzucił zamówienie " + event.orderId());
        }
    }

    @TransactionalEventListener
    public void afterCommit(AuditRequested event) {
        log.add("audyt:AFTER_COMMIT");
        if ("AFTER_COMMIT".equals(event.failIn())) {
            throw new IllegalStateException("Audyt po commicie zawiódł dla " + event.orderId());
        }
    }
}
