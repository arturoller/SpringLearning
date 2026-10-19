package s22_extras.ext05_webflux_overview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Ext05WebFluxOverview na ATRAPIE serwera (środowisko MOCK): WebTestClient woła aplikację bez sieci.
 * Typ aplikacji REACTIVE trzeba podać w {@code @SpringBootTest(properties = ...)} — test nie uruchamia
 * Ext05Application.main (tam jest setWebApplicationType), a sam Boot przy MVC i WebFlux na classpath wybiera MVC.
 */
@SpringBootTest(properties = "spring.main.web-application-type=reactive")
@AutoConfigureWebTestClient   // bean WebTestClient połączony z kontekstem aplikacji (bez prawdziwego serwera)
@ExtendWith(OutputCaptureExtension.class)
class Ext05WebFluxOverviewTest {

    @Autowired
    WebTestClient client;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("2. MVC i WebFlux na classpath: Boot domyślnie wybiera SERVLET (MVC) — dlatego lekcja ustawia REACTIVE")
    void servletWinsByDefault() {
        assertThat(new SpringApplication(Ext05Application.class).getWebApplicationType())
                .isEqualTo(WebApplicationType.SERVLET);
        assertThat(context.getBean(DispatcherHandler.class)).as("serce WebFlux zamiast DispatcherServlet").isNotNull();
        assertThat(context.getBeanNamesForType(org.springframework.web.servlet.DispatcherServlet.class)).isEmpty();
    }

    @Test
    @DisplayName("3. Flux<ReactiveProduct> → tablica JSON (4 produkty)")
    void fluxAsJsonArray() {
        client.get().uri("/api/reactive/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ReactiveProduct.class).hasSize(4)
                .contains(new ReactiveProduct("ELE-003", "Słuchawki BT", new BigDecimal("349.90"), 25));
    }

    @Test
    @DisplayName("3. Mono z produktem → 200; puste Mono + switchIfEmpty(Mono.error(...)) → 404")
    void monoAndNotFound() {
        client.get().uri("/api/reactive/products/SPO-001").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Kawa ziarnista 1kg");

        client.get().uri("/api/reactive/products/XXX-999").exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("3. Styl funkcyjny: RouterFunction /api/reactive/hello → 200 text/plain")
    void functionalRoute() {
        client.get().uri("/api/reactive/hello").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Cześć z WebFlux!");
    }

    @Test
    @DisplayName("4. block() wykryty także na atrapie: WebTestClient obsługuje zapytanie na nieblokującym wątku parallel-N → 500")
    void blockDetectedInMockEnvironment(CapturedOutput output) {
        client.get().uri("/api/reactive/products/ELE-001/blocking-stock").exchange()
                .expectStatus().is5xxServerError();

        assertThat(output.getAll()).containsPattern(
                "IllegalStateException: block\\(\\)/blockFirst\\(\\)/blockLast\\(\\) are blocking, which is not supported"
                        + " in thread parallel-\\d+");
    }

    @Test
    @DisplayName("4. Blokujący kod przez Mono.fromCallable(...).subscribeOn(boundedElastic) — liczony na wątku boundedElastic")
    void blockingCodeOnBoundedElastic() {
        client.get().uri("/api/reactive/products/ELE-001/wholesale-price").exchange()
                .expectStatus().isOk()
                .expectBody(ReactiveProductController.WholesalePrice.class)
                .value(price -> {
                    assertThat(price.price()).isEqualByComparingTo("4399.99");
                    assertThat(price.thread()).startsWith("boundedElastic-");
                });
    }
}
