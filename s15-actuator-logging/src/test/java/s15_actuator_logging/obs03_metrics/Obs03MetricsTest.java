package s15_actuator_logging.obs03_metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Obs03Metrics. Dobór narzędzia: metryki własne testujemy BEZ Springa — wystarczy
 * {@code new SimpleMeterRegistry()} (rejestr w pamięci; każdy test ma własny, więc liczniki się nie mieszają).
 * Metryki HTTP i punkt końcowy /actuator/metrics wymagają kontekstu — tam używamy @SpringBootTest + MockMvc
 * i porównujemy liczniki „przed i po” (kontekst jest współdzielony między testami). Nigdzie nie asercjonujemy
 * czasów — tylko liczby wywołań i wartości.
 */
class Obs03MetricsTest {

    private static NewOrder order(String channel) {
        return new NewOrder("ELE-001", 1, channel);
    }

    // =================================================================================================
    // 1. Counter — licznik i tagi
    // =================================================================================================

    @Test
    @DisplayName("1. Counter z tagiem channel: WEB i SKLEP to dwie niezależne serie o tej samej nazwie")
    void counterWithTags() {
        MeterRegistry registry = new SimpleMeterRegistry();
        OrderService service = new OrderService(registry);

        service.placeOrder(order("WEB"));
        service.placeOrder(order("WEB"));
        service.placeOrder(order("SKLEP"));

        assertThat(registry.get("orders.created").tag("channel", "WEB").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("orders.created").tag("channel", "SKLEP").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("orders.created").counters()).hasSize(2);
    }

    // =================================================================================================
    // 2. Timer — zliczamy wykonania, nie czas
    // =================================================================================================

    @Test
    @DisplayName("2. Timer: każde placeOrder to jedno wykonanie (count); czasu nie sprawdzamy")
    void timerCountsExecutions() {
        MeterRegistry registry = new SimpleMeterRegistry();
        OrderService service = new OrderService(registry);

        service.placeOrder(order("WEB"));
        service.placeOrder(order("WEB"));
        service.placeOrder(order("SKLEP"));

        Timer timer = registry.get("orders.processing").timer();
        assertThat(timer.count()).isEqualTo(3);
    }

    // =================================================================================================
    // 3. Gauge — wartość, która rośnie i maleje
    // =================================================================================================

    @Test
    @DisplayName("3. Gauge orders.open pokazuje bieżącą liczbę otwartych zamówień (rośnie i maleje)")
    void gaugeGoesUpAndDown() {
        MeterRegistry registry = new SimpleMeterRegistry();
        OrderService service = new OrderService(registry);
        assertThat(registry.get("orders.open").gauge().value()).isZero();

        service.placeOrder(order("WEB"));
        service.placeOrder(order("WEB"));
        assertThat(registry.get("orders.open").gauge().value()).isEqualTo(2.0);

        service.shipOrder();
        assertThat(registry.get("orders.open").gauge().value()).isEqualTo(1.0);
    }

    // =================================================================================================
    // 4. PUŁAPKA: wysoka kardynalność tagów
    // =================================================================================================

    @Test
    @DisplayName("4. Tag z identyfikatorem klienta tworzy tyle serii, ilu jest klientów; tag z kanałem — 2")
    void highCardinalityTag() {
        MeterRegistry registry = new SimpleMeterRegistry();

        for (int customerId = 1; customerId <= 5; customerId++) {
            // ŹLE: wartość tagu pochodzi z danych — każdy nowy klient to nowa seria w pamięci i w systemie metryk
            Counter.builder("orders.by.customer").tag("customerId", String.valueOf(customerId))
                    .register(registry).increment();
            // DOBRZE: wartość tagu z małego, ustalonego zbioru
            Counter.builder("orders.by.channel").tag("channel", customerId % 2 == 0 ? "WEB" : "SKLEP")
                    .register(registry).increment();
        }

        assertThat(registry.find("orders.by.customer").counters()).hasSize(5);   // rośnie z liczbą klientów
        assertThat(registry.find("orders.by.channel").counters()).hasSize(2);    // stała liczba
    }

    // =================================================================================================
    // 5. Boot: MeterRegistry jako bean, /actuator/metrics i metryki HTTP
    // =================================================================================================

    @Nested   // nested = zagnieżdżona (własny kontekst z ustawieniami)
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=metrics",
            "management.metrics.tags.application=sklep"})
    @AutoConfigureMockMvc
    @DisplayName("5. Boot: rejestr, punkt końcowy /actuator/metrics, metryki HTTP, wspólny tag")
    class WithSpring {

        @Autowired
        MockMvc mvc;
        @Autowired
        MeterRegistry registry;

        @Test
        @DisplayName("bez backendu metryk (np. Prometheus) Boot daje SimpleMeterRegistry — metryki żyją w pamięci")
        void registryIsInMemory() {
            assertThat(registry).isInstanceOf(SimpleMeterRegistry.class);
        }

        @Test
        @DisplayName("własna metryka po zapytaniu POST jest widoczna w /actuator/metrics/{nazwa}, z filtrem tagu")
        void ownMetricInEndpoint() throws Exception {
            mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sku\":\"SPO-001\",\"quantity\":2,\"channel\":\"KANAL-TESTOWY\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(startsWith("ZAM-")));

            mvc.perform(get("/actuator/metrics/orders.created").param("tag", "channel:KANAL-TESTOWY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("orders.created"))
                    .andExpect(jsonPath("$.measurements[0].statistic").value("COUNT"))
                    .andExpect(jsonPath("$.measurements[0].value").value(1.0));
            mvc.perform(get("/actuator/metrics/orders.created").param("tag", "channel:NIE-MA-TAKIEGO"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("lista /actuator/metrics zawiera metryki JVM i HTTP od Boota oraz nasze orders.*")
        void metricNamesList() throws Exception {
            mvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.names", hasItem("jvm.memory.used")))
                    .andExpect(jsonPath("$.names", hasItem("http.server.requests")))
                    .andExpect(jsonPath("$.names", hasItem("orders.open")))
                    .andExpect(jsonPath("$.names", hasItem("orders.processing")));
        }

        @Test
        @DisplayName("http.server.requests: tag uri to SZABLON (/api/orders/{id}), więc różne id to jedna seria")
        void httpMetricUsesUriTemplate() throws Exception {
            long before = httpCount("/api/orders/{id}");

            mvc.perform(get("/api/orders/ZAM-100")).andExpect(status().isOk());
            mvc.perform(get("/api/orders/ZAM-101")).andExpect(status().isOk());

            assertThat(httpCount("/api/orders/{id}")).isEqualTo(before + 2);
            mvc.perform(get("/actuator/metrics/http.server.requests"))
                    .andExpect(jsonPath("$.availableTags[?(@.tag=='uri')].values[*]", hasItem("/api/orders/{id}")))
                    .andExpect(jsonPath("$.availableTags[?(@.tag=='uri')].values[*]", not(hasItem("/api/orders/ZAM-100"))));
        }

        @Test
        @DisplayName("management.metrics.tags.application=sklep dokłada wspólny tag do każdej metryki")
        void commonTagIsApplied() throws Exception {
            mvc.perform(get("/actuator/metrics/orders.open"))
                    .andExpect(jsonPath("$.availableTags[?(@.tag=='application')].values[*]", contains("sklep")));
        }

        @Test
        @DisplayName("nieistniejąca metryka → 404")
        void unknownMetric() throws Exception {
            mvc.perform(get("/actuator/metrics/nie.ma.takiej")).andExpect(status().isNotFound());
        }

        private long httpCount(String uri) {
            Timer timer = registry.find("http.server.requests").tag("uri", uri).timer();
            return timer == null ? 0 : timer.count();
        }
    }
}
