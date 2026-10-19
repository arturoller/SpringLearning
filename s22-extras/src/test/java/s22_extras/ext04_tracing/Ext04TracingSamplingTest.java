package s22_extras.ext04_tracing;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import s22_extras.ext04_tracing.AvailabilityService.Availability;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext04Tracing, sekcja 7: próbkowanie 0.0 — śledzenie DZIAŁA (identyfikatory, propagacja, logi), ale żaden
 * odcinek nie jest zapisywany ani eksportowany.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=0.0")
@AutoConfigureTracing
@Import(Ext04TracingSamplingTest.InMemoryTracing.class)
@ExtendWith(OutputCaptureExtension.class)
class Ext04TracingSamplingTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class InMemoryTracing {
        @Bean
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    InMemorySpanExporter exporter;

    @Autowired
    SdkTracerProvider tracerProvider;

    @Test
    @DisplayName("7. Próbkowanie 0.0: traceparent z flagą 02 (nie zapisuj), zero odcinków w eksporterze, traceId nadal w logach")
    void notSampled(CapturedOutput output) {
        Availability availability = RestClient.create("http://localhost:" + port).get()
                .uri("/api/shop/ELE-001/availability").retrieve().body(Availability.class);
        tracerProvider.forceFlush().join(5, TimeUnit.SECONDS);

        assertThat(availability.stockSawTraceparent()).matches("00-" + availability.traceId() + "-[0-9a-f]{16}-02");
        // Niewybrane odcinki są „nienagrywające” — nigdy nie trafiają do procesora, więc brak ich jest pewny
        assertThat(exporter.getFinishedSpanItems()).isEmpty();
        assertThat(output.getAll()).contains("[" + availability.traceId() + "-");
    }
}
