package s17_events.solutions.evt01_application_events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import s17_events.evt01_application_events.Evt01ApplicationEvents.StockLow;
import s17_events.evt01_application_events.OrderPlaced;
import s17_events.evt01_application_events.PointsGranted;

import java.util.ArrayList;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Evt01ApplicationEvents — zajrzyj dopiero po własnej próbie! */
public final class Evt01ApplicationEventsSolutions {

    private Evt01ApplicationEventsSolutions() {
    }

    /** Rozwiązanie 1: wystarczy publishEvent z nowym rekordem. */
    public static void solution1(ApplicationEventPublisher publisher, String sku, int stock) {
        publisher.publishEvent(new StockLow(sku, stock));
    }

    /** Rozwiązanie 2: warunek w adnotacji — metoda w ogóle nie jest wywoływana dla zamówień poniżej 1000 zł. */
    public static class Solution2VipListener {
        public final List<String> vipOrderIds = new ArrayList<>();

        @EventListener(condition = "#event.totalPln >= 1000")
        public void onOrder(OrderPlaced event) {
            vipOrderIds.add(event.orderId());
        }
    }

    /** Rozwiązanie 3: @Order nad każdą metodą; kolejność deklaracji w klasie nie ma znaczenia. */
    public static class Solution3OrderedListeners {
        public final List<String> calls = new ArrayList<>();

        @Order(3)
        @EventListener
        public void third(OrderPlaced event) {
            calls.add("trzeci");
        }

        @Order(1)
        @EventListener
        public void first(OrderPlaced event) {
            calls.add("pierwszy");
        }

        @Order(2)
        @EventListener
        public void second(OrderPlaced event) {
            calls.add("drugi");
        }
    }

    /** Rozwiązanie 4: wartość zwrócona jest publikowana jako zdarzenie; null oznacza „nic nie publikuj”. */
    public static class Solution4PointsListener {
        @EventListener
        public PointsGranted onOrder(OrderPlaced event) {
            if (event.totalPln().intValue() < 100) {
                return null;
            }
            return new PointsGranted(event.customer(), event.totalPln().intValue() / 10);
        }
    }
}
