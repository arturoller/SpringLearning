package s15_actuator_logging.obs05_observations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Obs05Observations. Dobór narzędzia: logikę obserwacji sprawdzamy BEZ Springa na
 * {@code TestObservationRegistry} (rejestr z biblioteki micrometer-observation-test — zapamiętuje obserwacje, a
 * asercje pozwalają zajrzeć w ich nazwę, klucze i błąd). To, co Boot dokłada (metryka z obserwacji, własny handler,
 * obserwacja żądań HTTP, wyłączanie), sprawdzamy na prawdziwym kontekście lekcji.
 */
class Obs05ObservationsTest {

    // =================================================================================================
    // 1. Observation bez Springa: TestObservationRegistry
    // =================================================================================================

    @Test
    @DisplayName("1. observe(...) tworzy obserwację z nazwą i kluczami (niska i wysoka kardynalność) i ją kończy")
    void observationHasNameAndKeyValues() {
        TestObservationRegistry registry = TestObservationRegistry.create();

        String tracking = new DeliveryService(registry).ship("ZAM-001", "DHL");

        assertThat(tracking).isEqualTo("TRK-001");
        assertThat(registry)
                .hasSingleObservationThat()
                .hasNameEqualTo("deliveries.ship")
                .hasLowCardinalityKeyValue("carrier", "DHL")
                .hasHighCardinalityKeyValue("orderId", "ZAM-001")
                .hasBeenStopped();
    }

    @Test
    @DisplayName("1. wyjątek w observe(...) jest zapisany w obserwacji i rzucony dalej")
    void errorIsRecordedAndRethrown() {
        TestObservationRegistry registry = TestObservationRegistry.create();
        DeliveryService service = new DeliveryService(registry);

        assertThatThrownBy(() -> service.ship("ZAM-002", "NIEZNANY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nieznany przewoźnik: NIEZNANY");

        assertThat(registry)
                .hasSingleObservationThat()
                .hasNameEqualTo("deliveries.ship")
                .assertThatError().isInstanceOf(IllegalArgumentException.class);
        assertThat(registry).hasSingleObservationThat().hasBeenStopped();
    }

    @Test
    @DisplayName("1. ObservationRegistry.NOOP: kod działa normalnie, tylko nikt go nie obserwuje")
    void noopRegistryStillRunsTheCode() {
        String result = Observation.createNotStarted("cokolwiek", ObservationRegistry.NOOP).observe(() -> "wynik");

        assertThat(result).isEqualTo("wynik");
        assertThat(ObservationRegistry.NOOP.isNoop()).isTrue();
    }

    // =================================================================================================
    // 2. Boot: obserwacja zamienia się w metrykę, handler własny, żądania HTTP
    // =================================================================================================

    @Nested   // nested = zagnieżdżona (własny kontekst)
    @SpringBootTest(properties = "management.endpoints.web.exposure.include=metrics")
    @AutoConfigureMockMvc
    @DisplayName("2. Boot: jedna obserwacja = metryka + wywołania handlerów")
    class WithSpring {

        @Autowired
        DeliveryService service;
        @Autowired
        MeterRegistry meterRegistry;
        @Autowired
        RecordingObservationHandler handler;
        @Autowired
        MockMvc mvc;

        @BeforeEach   // before each = przed każdym testem (kontekst jest współdzielony — czyścimy zapis zdarzeń)
        void clearHandler() {
            handler.clear();
        }

        @Test
        @DisplayName("obserwacja deliveries.ship staje się czasomierzem z tagiem carrier; orderId NIE jest tagiem")
        void observationBecomesTimer() {
            service.ship("ZAM-100", "INPOST");
            service.ship("ZAM-101", "INPOST");

            Timer timer = meterRegistry.get("deliveries.ship").tag("carrier", "INPOST").timer();
            assertThat(timer.count()).isEqualTo(2);
            assertThat(timer.getId().getTags()).extracting(tag -> tag.getKey()).contains("carrier", "error")
                    .doesNotContain("orderId");
            assertThat(timer.getId().getTag("error")).isEqualTo("none");
        }

        @Test
        @DisplayName("błąd w obserwowanym kodzie trafia do tagu error (nazwa klasy wyjątku)")
        void errorBecomesTag() {
            assertThatThrownBy(() -> service.ship("ZAM-102", "KURIER-WIDMO"))
                    .isInstanceOf(IllegalArgumentException.class);

            Timer timer = meterRegistry.get("deliveries.ship").tag("carrier", "KURIER-WIDMO").timer();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.getId().getTag("error")).isEqualTo("IllegalArgumentException");
        }

        @Test
        @DisplayName("własny bean ObservationHandler jest rejestrowany sam: widzi start i stop, a przy błędzie — error")
        void customHandlerIsCalled() {
            service.ship("ZAM-103", "POCZTA");
            assertThatThrownBy(() -> service.ship("ZAM-104", "NIEZNANY")).isInstanceOf(IllegalArgumentException.class);

            assertThat(handler.events()).containsExactly(
                    "start:deliveries.ship", "stop:deliveries.ship",
                    "start:deliveries.ship", "error:deliveries.ship:IllegalArgumentException", "stop:deliveries.ship");
        }

        @Test
        @DisplayName("Boot sam obserwuje żądania HTTP: ten sam handler widzi http.server.requests")
        void bootObservesHttpRequests() throws Exception {
            mvc.perform(post("/api/deliveries").param("orderId", "ZAM-105").param("carrier", "DHL"))
                    .andExpect(status().isOk());

            assertThat(handler.events()).containsSubsequence("start:http.server.requests", "start:deliveries.ship",
                    "stop:deliveries.ship", "stop:http.server.requests");
        }
    }

    // =================================================================================================
    // 3. Wyłączanie obserwacji po przedrostku nazwy
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = "management.observations.enable.deliveries=false")
    @DisplayName("3. management.observations.enable.<przedrostek>=false wyłącza obserwacje o takiej nazwie")
    class Disabled {

        @Autowired
        DeliveryService service;
        @Autowired
        MeterRegistry meterRegistry;
        @Autowired
        RecordingObservationHandler handler;

        @Test
        @DisplayName("kod się wykonuje, ale nie ma metryki ani wywołań handlera")
        void codeRunsButNothingIsObserved() {
            handler.clear();

            String tracking = service.ship("ZAM-200", "DHL");

            assertThat(tracking).startsWith("TRK-");
            assertThat(meterRegistry.find("deliveries.ship").timer()).isNull();
            assertThat(handler.events()).isEmpty();
        }
    }
}
