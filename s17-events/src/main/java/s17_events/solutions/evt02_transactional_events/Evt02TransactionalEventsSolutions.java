package s17_events.solutions.evt02_transactional_events;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import s17_events.evt02_transactional_events.OrderPlaced;

import java.util.ArrayList;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Evt02TransactionalEvents — zajrzyj dopiero po własnej próbie! */
public final class Evt02TransactionalEventsSolutions {

    private Evt02TransactionalEventsSolutions() {
    }

    /** Rozwiązanie 1: domyślna faza to AFTER_COMMIT, więc wystarczy sama adnotacja. */
    public static class Solution1ConfirmedListener {
        public final List<String> confirmed = new ArrayList<>();

        @TransactionalEventListener
        public void onOrder(OrderPlaced event) {
            confirmed.add(event.orderId());
        }
    }

    /** Rozwiązanie 2: faza AFTER_ROLLBACK — słuchacz wykona się tylko po wycofaniu. */
    public static class Solution2CancelledListener {
        public final List<String> cancelled = new ArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
        public void onOrder(OrderPlaced event) {
            cancelled.add(event.orderId());
        }
    }

    /** Rozwiązanie 3: fallbackExecution = true — gdy transakcji nie ma, słuchacz wykonuje się od razu. */
    public static class Solution3FallbackListener {
        public final List<String> received = new ArrayList<>();

        @TransactionalEventListener(fallbackExecution = true)
        public void onOrder(OrderPlaced event) {
            received.add(event.orderId());
        }
    }

    /** Rozwiązanie 4: BEFORE_COMMIT + wyjątek = weto; transakcja zostaje wycofana. */
    public static class Solution4LimitListener {
        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onOrder(OrderPlaced event) {
            if (event.totalPln().intValue() > 10000) {
                throw new IllegalStateException("Limit przekroczony");
            }
        }
    }
}
