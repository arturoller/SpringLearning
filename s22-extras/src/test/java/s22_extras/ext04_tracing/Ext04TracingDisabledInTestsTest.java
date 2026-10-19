package s22_extras.ext04_tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import s22_extras.ext04_tracing.AvailabilityService.Availability;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext04Tracing, sekcja 8 (PUŁAPKA): test BEZ {@code @AutoConfigureTracing}. Starter
 * spring-boot-starter-opentelemetry-test ustawia wtedy management.tracing.export.enabled=false — Boot nie eksportuje
 * i NIE PROPAGUJE śladów. Odcinki lokalne i identyfikatory w logach nadal są, ale „ślad rozpada się” na dwa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=1.0")
@ExtendWith(OutputCaptureExtension.class)
class Ext04TracingDisabledInTestsTest {

    @LocalServerPort
    int port;

    @Autowired
    Environment environment;

    @Test
    @DisplayName("8. Bez @AutoConfigureTracing: export.enabled=false, brak traceparent, magazyn loguje INNY traceId")
    void traceBreaksWithoutAutoConfigureTracing(CapturedOutput output) {
        Availability availability = RestClient.create("http://localhost:" + port).get()
                .uri("/api/shop/ELE-001/availability").retrieve().body(Availability.class);

        assertThat(environment.getProperty("management.tracing.export.enabled")).isEqualTo("false");
        assertThat(availability.stockSawTraceparent()).isNull();
        String stockLogLine = output.getAll().lines()
                .filter(line -> line.contains("Magazyn: sprawdzam stan ELE-001")).findFirst().orElseThrow();
        assertThat(stockLogLine).doesNotContain(availability.traceId());   // magazyn zaczął WŁASNY ślad
    }
}
