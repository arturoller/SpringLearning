package s17_events.solutions.evt01_application_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import s17_events.evt01_application_events.Evt01ApplicationEvents.StockLow;
import s17_events.evt01_application_events.OrderPlaced;
import s17_events.evt01_application_events.PointsGranted;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Evt01ApplicationEventsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Evt01ApplicationEventsSolutionsTest {

    static class Collector {
        final List<Object> received = new ArrayList<>();

        @EventListener
        void onStockLow(StockLow event) {
            received.add(event);
        }

        @EventListener
        void onPoints(PointsGranted event) {
            received.add(event);
        }
    }

    private static OrderPlaced order(String id, String total) {
        return new OrderPlaced(id, "Jan Kowalski", new BigDecimal(total));
    }

    @Test
    @DisplayName("Rozwiązanie 1: opublikowanie zdarzenia StockLow")
    void solution1() {
        try (var context = new AnnotationConfigApplicationContext(Collector.class)) {
            Evt01ApplicationEventsSolutions.solution1(context, "ELE-002", 0);
            assertThat(context.getBean(Collector.class).received).containsExactly(new StockLow("ELE-002", 0));
        }
    }

    @Test
    @DisplayName("Rozwiązanie 2: słuchacz VIP reaguje tylko od 1000 zł")
    void solution2() {
        try (var context = new AnnotationConfigApplicationContext(Evt01ApplicationEventsSolutions.Solution2VipListener.class)) {
            context.publishEvent(order("ZAM-001", "349.90"));
            context.publishEvent(order("ZAM-002", "5499.99"));
            assertThat(context.getBean(Evt01ApplicationEventsSolutions.Solution2VipListener.class).vipOrderIds)
                    .containsExactly("ZAM-002");
        }
    }

    @Test
    @DisplayName("Rozwiązanie 3: kolejność słuchaczy z @Order")
    void solution3() {
        try (var context = new AnnotationConfigApplicationContext(Evt01ApplicationEventsSolutions.Solution3OrderedListeners.class)) {
            context.publishEvent(order("ZAM-001", "349.90"));
            assertThat(context.getBean(Evt01ApplicationEventsSolutions.Solution3OrderedListeners.class).calls)
                    .containsExactly("pierwszy", "drugi", "trzeci");
        }
    }

    @Test
    @DisplayName("Rozwiązanie 4: zdarzenie zwrócone ze słuchacza, a poniżej 100 zł — nic")
    void solution4() {
        try (var context = new AnnotationConfigApplicationContext(
                Evt01ApplicationEventsSolutions.Solution4PointsListener.class, Collector.class)) {
            context.publishEvent(order("ZAM-001", "349.90"));
            context.publishEvent(order("ZAM-002", "49.99"));
            assertThat(context.getBean(Collector.class).received).containsExactly(new PointsGranted("Jan Kowalski", 34));
        }
    }
}
