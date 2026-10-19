package s17_events.evt01_application_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Evt01ApplicationEvents. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Zwykły test jednostkowy z małym kontekstem Springa (AnnotationConfigApplicationContext) — pełny Boot jest tu zbędny.
 */
@Tag("cwiczenie")
class Evt01ApplicationEventsExercisesTest {

    /** Zbiera zdarzenia, żeby testy mogły sprawdzić, co dotarło. */
    static class Collector {
        final List<Object> received = new ArrayList<>();

        @EventListener
        void onStockLow(Evt01ApplicationEvents.StockLow event) {
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
    @DisplayName("Ćwiczenie 1: opublikowanie zdarzenia StockLow")
    void exercise1() {
        try (var context = new AnnotationConfigApplicationContext(Collector.class)) {
            Evt01ApplicationEvents.exercise1(context, "ELE-002", 0);
            assertThat(context.getBean(Collector.class).received)
                    .containsExactly(new Evt01ApplicationEvents.StockLow("ELE-002", 0));
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: słuchacz VIP reaguje tylko od 1000 zł")
    void exercise2() {
        try (var context = new AnnotationConfigApplicationContext(Evt01ApplicationEvents.Exercise2VipListener.class)) {
            context.publishEvent(order("ZAM-001", "349.90"));
            context.publishEvent(order("ZAM-002", "5499.99"));
            assertThat(context.getBean(Evt01ApplicationEvents.Exercise2VipListener.class).vipOrderIds)
                    .containsExactly("ZAM-002");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: kolejność słuchaczy z @Order")
    void exercise3() {
        try (var context = new AnnotationConfigApplicationContext(Evt01ApplicationEvents.Exercise3OrderedListeners.class)) {
            context.publishEvent(order("ZAM-001", "349.90"));
            assertThat(context.getBean(Evt01ApplicationEvents.Exercise3OrderedListeners.class).calls)
                    .containsExactly("pierwszy", "drugi", "trzeci");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: zdarzenie zwrócone ze słuchacza, a poniżej 100 zł — nic")
    void exercise4() {
        try (var context = new AnnotationConfigApplicationContext(
                Evt01ApplicationEvents.Exercise4PointsListener.class, Collector.class)) {
            context.publishEvent(order("ZAM-001", "349.90"));
            context.publishEvent(order("ZAM-002", "49.99"));
            assertThat(context.getBean(Collector.class).received)
                    .containsExactly(new PointsGranted("Jan Kowalski", 34));
        }
    }
}
