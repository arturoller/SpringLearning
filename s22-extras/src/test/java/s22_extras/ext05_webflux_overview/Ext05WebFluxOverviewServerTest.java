package s22_extras.ext05_webflux_overview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext05WebFluxOverview na PRAWDZIWYM serwerze Netty (RANDOM_PORT). Tylko tu kontroler działa na wątku pętli
 * zdarzeń — i tylko tu Reactor wykrywa block().
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.web-application-type=reactive")
@ExtendWith(OutputCaptureExtension.class)
class Ext05WebFluxOverviewServerTest {

    @LocalServerPort
    int port;

    @Autowired
    ReactiveWebServerApplicationContext context;

    WebTestClient client;

    @BeforeEach
    void bindToServer() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();   // prawdziwa sieć
    }

    @Test
    @DisplayName("2. Aplikacja reaktywna: serwer Netty (nie Tomcat)")
    void nettyServer() {
        assertThat(context.getWebServer().getClass().getSimpleName()).isEqualTo("NettyWebServer");
    }

    @Test
    @DisplayName("4. PUŁAPKA: block() na wątku Netty → 500 i IllegalStateException „... not supported in thread ...-http-nio-N”")
    void blockOnEventLoop(CapturedOutput output) {
        client.get().uri("/api/reactive/products/ELE-001/blocking-stock").exchange()
                .expectStatus().is5xxServerError();

        assertThat(output.getAll()).containsPattern(
                "IllegalStateException: block\\(\\)/blockFirst\\(\\)/blockLast\\(\\) are blocking, which is not supported"
                        + " in thread [a-z]+-http-nio-\\d+");   // aplikacja: reactor-http-nio-4, ten test: webflux-http-nio-4
    }

    @Test
    @DisplayName("4. Ten sam blokujący kod przeniesiony na boundedElastic działa także na prawdziwym serwerze")
    void boundedElasticOnRealServer() {
        client.get().uri("/api/reactive/products/ELE-003/wholesale-price").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.thread").value(thread -> assertThat((String) thread).startsWith("boundedElastic-"));
    }
}
