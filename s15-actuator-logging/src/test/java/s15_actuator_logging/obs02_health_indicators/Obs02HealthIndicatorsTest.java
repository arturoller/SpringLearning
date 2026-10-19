package s15_actuator_logging.obs02_health_indicators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.actuate.endpoint.SimpleHttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.SimpleStatusAggregator;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Obs02HealthIndicators. Dobór narzędzia: sam wskaźnik to zwykła klasa — testujemy ją bez Springa;
 * odpowiedzi /actuator/health sprawdzamy MockMvc na prawdziwym kontekście lekcji (Obs02Application).
 */
class Obs02HealthIndicatorsTest {

    // =================================================================================================
    // 1. Wskaźnik jako zwykła klasa — bez Springa
    // =================================================================================================

    @Test
    @DisplayName("1. WarehouseHealthIndicator: UP ze szczegółem, gdy magazyn działa, i DOWN z powodem, gdy nie")
    void indicatorIsPlainJava() {
        Warehouse warehouse = new Warehouse();
        WarehouseHealthIndicator indicator = new WarehouseHealthIndicator(warehouse);

        Health up = indicator.health();
        assertThat(up.getStatus()).isEqualTo(Status.UP);
        assertThat(up.getDetails()).containsEntry("items", 14);

        warehouse.setAvailable(false);
        Health down = indicator.health();
        assertThat(down.getStatus()).isEqualTo(Status.DOWN);
        assertThat(down.getDetails()).containsEntry("reason", "magazyn nie odpowiada");
    }

    // =================================================================================================
    // 2. AbstractHealthIndicator — wyjątek zamienia się w DOWN
    // =================================================================================================

    /** Wskaźnik, który „wywala się” przy sprawdzaniu (np. baza nie odpowiada). */
    static class BrokenIndicator extends AbstractHealthIndicator {
        @Override
        protected void doHealthCheck(Health.Builder builder) {
            throw new IllegalStateException("brak połączenia z bazą");
        }
    }

    @Test
    @DisplayName("2. AbstractHealthIndicator: wyjątek w doHealthCheck daje DOWN i szczegół error")
    void exceptionBecomesDown() {
        Health health = new BrokenIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "java.lang.IllegalStateException: brak połączenia z bazą");
    }

    // =================================================================================================
    // 3. Domyślnie: bez szczegółów
    // =================================================================================================

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("3. Domyślnie show-details=never — widać tylko status")
    class DefaultsNoDetails {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("odpowiedź health ma status UP i nie ma sekcji components")
        void noComponents() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
    }

    // =================================================================================================
    // 4. Szczegóły, własny wskaźnik, grupy, sondy — kontekst z ustawieniami lekcji
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=health",
            "management.endpoint.health.show-details=always",
            "management.endpoint.health.group.sklep.include=warehouse,ping",
            "management.endpoint.health.group.sklep.show-details=always"})
    @AutoConfigureMockMvc
    @DisplayName("4. Własny wskaźnik, szczegóły, grupa i sondy")
    class WithDetails {

        @Autowired
        MockMvc mvc;
        @Autowired
        Warehouse warehouse;
        @Autowired
        ApplicationContext context;
        @Autowired
        ApplicationAvailability availability;

        @BeforeEach   // before each = przed każdym testem (współdzielony kontekst — przywracamy stan wyjściowy)
        void reset() {
            warehouse.setAvailable(true);
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }

        @Test
        @DisplayName("własny wskaźnik pojawia się jako składnik warehouse, obok wbudowanych")
        void customIndicatorAppears() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components.warehouse.status").value("UP"))
                    .andExpect(jsonPath("$.components.warehouse.details.items").value(14))
                    .andExpect(jsonPath("$.components.ping.status").value("UP"))
                    .andExpect(jsonPath("$.components.diskSpace.status").value("UP"));
        }

        @Test
        @DisplayName("jeden wskaźnik DOWN zmienia status całości na DOWN i kod HTTP na 503")
        void oneDownMakesEverythingDown() throws Exception {
            warehouse.setAvailable(false);

            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.components.warehouse.status").value("DOWN"))
                    .andExpect(jsonPath("$.components.warehouse.details.reason").value("magazyn nie odpowiada"))
                    .andExpect(jsonPath("$.components.ping.status").value("UP"));
        }

        @Test
        @DisplayName("grupa sklep: /actuator/health/sklep zawiera tylko warehouse i ping")
        void groupContainsOnlyChosenIndicators() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(jsonPath("$.groups", hasItems("liveness", "readiness", "sklep")));
            mvc.perform(get("/actuator/health/sklep"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components.warehouse.status").value("UP"))
                    .andExpect(jsonPath("$.components.ping.status").value("UP"))
                    .andExpect(jsonPath("$.components.diskSpace").doesNotExist());
        }

        @Test
        @DisplayName("sondy liveness i readiness są domyślnie włączone i mają stan UP")
        void probesAreEnabledByDefault() throws Exception {
            mvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
            mvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("zepsuty magazyn NIE wpływa na sondy — liveness i readiness nadal UP")
        void brokenWarehouseDoesNotTouchProbes() throws Exception {
            warehouse.setAvailable(false);

            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("AvailabilityChangeEvent: REFUSING_TRAFFIC → readiness 503 (OUT_OF_SERVICE), liveness nadal 200")
        void refusingTrafficChangesReadinessOnly() throws Exception {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);

            assertThat(availability.getReadinessState()).isEqualTo(ReadinessState.REFUSING_TRAFFIC);
            assertThat(availability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
            mvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
            mvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk());
        }
    }

    // =================================================================================================
    // 6. Jak Boot zamienia statusy na wynik zbiorczy i na kod HTTP (prawdziwe klasy Boota, bez kontekstu)
    // =================================================================================================

    @Test
    @DisplayName("6. Kody HTTP: DOWN i OUT_OF_SERVICE → 503, UP i UNKNOWN → 200")
    void defaultHttpCodes() {
        SimpleHttpCodeStatusMapper mapper = new SimpleHttpCodeStatusMapper();

        assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(200);
        assertThat(mapper.getStatusCode(Status.UNKNOWN)).isEqualTo(200);
        assertThat(mapper.getStatusCode(Status.DOWN)).isEqualTo(503);
        assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE)).isEqualTo(503);
    }

    @Test
    @DisplayName("6. Status zbiorczy: wygrywa najgorszy w kolejności DOWN, OUT_OF_SERVICE, UP, UNKNOWN")
    void aggregateStatus() {
        SimpleStatusAggregator aggregator = new SimpleStatusAggregator();

        assertThat(aggregator.getAggregateStatus(Status.UP, Status.UP)).isEqualTo(Status.UP);
        assertThat(aggregator.getAggregateStatus(Status.UP, Status.UNKNOWN)).isEqualTo(Status.UP);
        assertThat(aggregator.getAggregateStatus(Status.UP, Status.OUT_OF_SERVICE)).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(aggregator.getAggregateStatus(Status.OUT_OF_SERVICE, Status.DOWN, Status.UP)).isEqualTo(Status.DOWN);
        assertThat(aggregator.getAggregateStatus()).isEqualTo(Status.UNKNOWN);
    }

    // =================================================================================================
    // 5. Wyłączenie sond
    // =================================================================================================

    @Nested
    @SpringBootTest(properties = {
            "management.endpoints.web.exposure.include=health",
            "management.endpoint.health.probes.enabled=false"})
    @AutoConfigureMockMvc
    @DisplayName("5. management.endpoint.health.probes.enabled=false wyłącza sondy")
    class ProbesDisabled {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("adresy liveness i readiness dają 404")
        void probesGone() throws Exception {
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isNotFound());
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isNotFound());
        }
    }
}
