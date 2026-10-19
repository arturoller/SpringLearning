package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 4 lekcji Rest01Controllers: błędy mapowania, których nie da się pokazać w działającym kontekście lekcji.
 * Każdy test startuje OSOBNĄ, prawdziwą aplikację (SpringApplicationBuilder) z celowo zepsutym kontrolerem.
 *
 * <p>To zwykła klasa testowa (bez {@code @SpringBootTest}) — gdyby miała {@code @SpringBootTest}, zagnieżdżone klasy
 * {@code @Configuration} zostałyby wzięte jako konfiguracja testu. Zagnieżdżone klasy testów nie są też widoczne dla
 * skanowania komponentów lekcji (Boot pomija je w testach filtrem TestTypeExcludeFilter).</p>
 */
class Rest01ControllersStartupTest {

    @Test
    @DisplayName("4c. Dwie metody z tym samym adresem i metodą HTTP → aplikacja NIE startuje: Ambiguous mapping (pytanie 2)")
    void duplicateMappingStopsStartup() {
        assertThatThrownBy(() -> start(DuplicateMappingApp.class).close())
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Ambiguous mapping. Cannot map 'productController' method")
                .hasMessageContaining("to {GET [/api/products/{sku}]}: There is already 'productController' bean method");
    }

    @Test
    @DisplayName("4d. Wzorce {sku} i {id} na tym samym adresie → start się udaje, ale zapytanie kończy się 500")
    void differentPatternsSameAddressFailAtRequestTime() throws Exception {
        try (ConfigurableApplicationContext app = start(AmbiguousPatternsApp.class)) {
            int port = app.getEnvironment().getRequiredProperty("local.server.port", Integer.class);

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/x/1")).build(),
                    HttpResponse.BodyHandlers.ofString());

            // w logu serwera: IllegalStateException: Ambiguous handler methods mapped for '/api/x/1': {...}
            assertThat(response.statusCode()).isEqualTo(500);
        }
    }

    /** Startuje aplikację na losowym porcie (server.port=0) — nigdy na stałym 8080. */
    private static ConfigurableApplicationContext start(Class<?> config) {
        return new SpringApplicationBuilder(config)
                .properties("server.port=0", "spring.main.banner-mode=off")
                .run();
    }

    @Configuration
    @EnableAutoConfiguration
    static class DuplicateMappingApp {

        @Bean   // nazwa beana = nazwa metody, jak przy skanowaniu ProductController
        DuplicateMappingController productController() {
            return new DuplicateMappingController();
        }
    }

    /** Kopia fragmentu ProductController z DOPISANĄ drugą metodą na GET /api/products/{sku}. */
    @RestController
    @RequestMapping("/api/products")
    static class DuplicateMappingController {

        @GetMapping("/{sku}")
        String one(@PathVariable String sku) {
            return "one";
        }

        @GetMapping("/{sku}")   // ten sam adres i ta sama metoda HTTP — duplikat
        String oneAgain(@PathVariable String sku) {
            return "oneAgain";
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class AmbiguousPatternsApp {

        @Bean
        AmbiguousPatternsController ambiguousPatternsController() {
            return new AmbiguousPatternsController();
        }
    }

    @RestController
    @RequestMapping("/api/x")
    static class AmbiguousPatternsController {

        @GetMapping("/{sku}")
        String bySku(@PathVariable String sku) {
            return "sku";
        }

        @GetMapping("/{id}")   // inny wzorzec, ale pasuje do TYCH SAMYCH adresów
        String byId(@PathVariable String id) {
            return "id";
        }
    }
}
