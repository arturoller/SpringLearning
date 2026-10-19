package s19_deploy.dep04_startup_and_shutdown;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy sond liveness/readiness (lekcja Dep04StartupAndShutdown, sekcja 3). Wycinek: pełny kontekst + MockMvc —
 * punkty Actuatora są częścią kontekstu webowego, a sondy nie potrzebują prawdziwego portu.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Dep04StartupAndShutdownProbesTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ApplicationContext context;

    @AfterEach
    void restoreHealthyState() {
        // kontekst jest współdzielony między testami klasy — przywracamy stany początkowe
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
    }

    @Test
    @DisplayName("3. Sondy są włączone domyślnie (bez Kubernetesa): health ma grupy liveness i readiness, oba UP")
    void probesEnabledByDefault() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("UP"))
                .hasPathSatisfying("$.groups", groups -> assertThat(groups).isEqualTo(java.util.List.of("liveness", "readiness")));
        assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk().bodyJson().isLenientlyEqualTo("{\"status\":\"UP\"}");
        assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk().bodyJson().isLenientlyEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    @DisplayName("3. REFUSING_TRAFFIC: readiness odpowiada 503 OUT_OF_SERVICE, a liveness nadal 200 UP")
    void readinessRefusingTraffic() {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);

        assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatus(503).bodyJson()
                .isLenientlyEqualTo("{\"status\":\"OUT_OF_SERVICE\"}");
        assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
    }

    @Test
    @DisplayName("3. LivenessState.BROKEN: liveness odpowiada 503 DOWN — platforma powinna zrestartować aplikację")
    void livenessBroken() {
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);

        assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatus(503).bodyJson()
                .isLenientlyEqualTo("{\"status\":\"DOWN\"}");
    }

    @Test
    @DisplayName("3. Tylko health jest wystawiony przez HTTP — env i info nie (domyślnie w Boot 4.1)")
    void onlyHealthIsExposed() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(404);
        assertThat(mvc.get().uri("/actuator/info")).hasStatus(404);
    }
}
