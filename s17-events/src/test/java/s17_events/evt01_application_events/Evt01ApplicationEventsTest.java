package s17_events.evt01_application_events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Evt01ApplicationEvents. Używamy {@code @SpringBootTest}, bo słuchacze są beanami w kontekście
 * Springa (własna klasa startowa lekcji: Evt01Application). Każda sekcja lekcji ma jedną metodę testową.
 */
@SpringBootTest
@ActiveProfiles("evt01")
class Evt01ApplicationEventsTest {

    @Autowired
    private OrderService service;
    @Autowired
    private EventLog log;
    @Autowired
    private ConfigurableApplicationContext context;

    @BeforeEach
    void setUp() {
        log.clear();
    }

    @Test
    @DisplayName("1. Publikacja zwykłego rekordu: słuchacze dostają zdarzenie, a Spring opakowuje je w PayloadApplicationEvent")
    void section1() {
        List<Object> payloads = new ArrayList<>();
        ApplicationListener<PayloadApplicationEvent<?>> probe = event -> payloads.add(event.getPayload());
        context.addApplicationListener(probe);

        service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));

        assertThat(payloads).contains(new OrderPlaced("ZAM-001", "Jan Kowalski", new BigDecimal("349.90")));
        assertThat(log.whats()).contains("magazyn:ZAM-001", "punkty:ZAM-001", "powiadomienie:ZAM-001");
    }

    @Test
    @DisplayName("2. Słuchacze działają SYNCHRONICZNIE: w wątku publikującego i przed zakończeniem publishEvent")
    void section2() {
        service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));

        assertThat(log.whats()).last().isEqualTo("publikacja-zakończona:ZAM-001");
        assertThat(log.entries()).extracting(EventLog.Entry::thread)
                .containsOnly(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("3. Wyjątek słuchacza wraca do publikującego, a kolejni słuchacze się nie wykonują")
    void section3() {
        assertThatThrownBy(() -> service.publishBroken("test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Słuchacz zepsuty: test");

        assertThat(log.whats()).isEmpty();   // ani drugi słuchacz, ani linia po publikacji nie zdążyły się wykonać
    }

    @Test
    @DisplayName("4. @Order ustala kolejność słuchaczy tego samego zdarzenia (mniejsza liczba = wcześniej)")
    void section4() {
        service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));

        assertThat(log.whats()).filteredOn(w -> w.endsWith(":ZAM-001") && !w.startsWith("zamówienie")
                        && !w.startsWith("publikacja"))
                .containsExactly("magazyn:ZAM-001", "punkty:ZAM-001", "powiadomienie:ZAM-001");
    }

    @Test
    @DisplayName("5. condition (SpEL): słuchacz „duże zamówienie” reaguje tylko od 1000 zł")
    void section5() {
        service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));
        assertThat(log.whats()).noneMatch(w -> w.startsWith("duże-zamówienie"));

        service.placeOrder("ZAM-002", "Zofia Krawczyk", new BigDecimal("5499.99"));
        assertThat(log.whats()).contains("duże-zamówienie:ZAM-002");
    }

    @Test
    @DisplayName("6. Słuchacz zwracający zdarzenie: Spring publikuje je od razu po zakończeniu tej metody")
    void section6() {
        service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));

        assertThat(log.whats()).containsSubsequence(
                "punkty:ZAM-001", "punkty-zapisane:Jan Kowalski=34", "powiadomienie:ZAM-001");
    }

    /** Zdarzenie, na które nikt nie nasłuchuje. */
    record Unheard(String text) {
    }

    /** Klasa z @EventListener, ale NIE będąca beanem — Spring jej nie zna. */
    static class NotABean {
        final List<String> received = new ArrayList<>();

        @org.springframework.context.event.EventListener
        void on(OrderPlaced event) {
            received.add(event.orderId());
        }
    }

    /** Słuchacz z błędnym warunkiem: brakuje # przed nazwą parametru. */
    static class BadCondition {
        @org.springframework.context.event.EventListener(condition = "event.totalPln >= 1000")
        void on(OrderPlaced event) {
        }
    }

    /** Słuchacz zwracający LISTĘ zdarzeń oraz zbierający punkty. */
    static class ListReturning {
        final List<PointsGranted> seen = new ArrayList<>();

        @org.springframework.context.event.EventListener
        List<PointsGranted> on(Unheard event) {
            return List.of(new PointsGranted("Jan Kowalski", 1), new PointsGranted("Jan Kowalski", 2));
        }

        @org.springframework.context.event.EventListener
        void seenPoints(PointsGranted event) {
            seen.add(event);
        }
    }

    @Test
    @DisplayName("6b. Słuchacz zwracający listę: każdy element jest publikowany jako osobne zdarzenie")
    void section6b() {
        try (var small = new org.springframework.context.annotation.AnnotationConfigApplicationContext(ListReturning.class)) {
            small.publishEvent(new Unheard("start"));
            assertThat(small.getBean(ListReturning.class).seen)
                    .containsExactly(new PointsGranted("Jan Kowalski", 1), new PointsGranted("Jan Kowalski", 2));
        }
    }

    @Test
    @DisplayName("7. Zdarzenie bez słuchaczy nie jest błędem, a obiekt spoza kontekstu (new) nie słucha")
    void section7() {
        context.publishEvent(new Unheard("nikt tego nie czyta"));   // brak wyjątku

        NotABean notABean = new NotABean();
        service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));
        assertThat(notABean.received).isEmpty();
    }

    @Test
    @DisplayName("8. Warunek bez znaku # kończy się wyjątkiem SpEL dopiero przy publikacji zdarzenia")
    void section8() {
        try (var small = new org.springframework.context.annotation.AnnotationConfigApplicationContext(BadCondition.class)) {
            assertThatThrownBy(() -> small.publishEvent(new OrderPlaced("ZAM-001", "Jan Kowalski", BigDecimal.TEN)))
                    .isInstanceOf(org.springframework.expression.spel.SpelEvaluationException.class)
                    .hasMessageContaining("EL1008E: Property or field 'totalPln' cannot be found on object of type "
                            + "'org.springframework.context.PayloadApplicationEvent'");
        }
    }
}
