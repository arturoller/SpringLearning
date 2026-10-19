package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 3–5 lekcji Rest07ApiVersioning: inne strategie i ustawienia niż w aplikacji lekcji. W jednej aplikacji
 * jest JEDNA strategia wersjonowania, więc każdy test startuje osobną, małą, prawdziwą aplikację (losowy port)
 * z własną konfiguracją i sprawdza ją zwykłym klientem HTTP z JDK.
 * Zwykła klasa testowa (bez {@code @SpringBootTest}) — zagnieżdżone {@code @Configuration} nie są konfiguracją testu.
 */
class Rest07ApiVersioningStrategiesTest {

    private final HttpClient client = HttpClient.newHttpClient();

    private static ConfigurableApplicationContext start(Class<?> config, String... properties) {
        return new SpringApplicationBuilder(config)
                .properties("server.port=0", "spring.main.banner-mode=off")
                .properties(properties)
                .run();
    }

    private HttpResponse<String> get(ConfigurableApplicationContext app, String path, String... headers)
            throws Exception {
        int port = app.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("3. Wersja w ŚCIEŻCE: usePathSegment(1) — /api/v1/… i /api/v2/… to jeden wzorzec /api/{version}/…")
    void pathSegment() throws Exception {
        try (ConfigurableApplicationContext app = start(PathSegmentApp.class)) {
            assertThat(get(app, "/api/v1/catalog/ELE-001").body()).isEqualTo("v1: ELE-001");
            assertThat(get(app, "/api/v2/catalog/ELE-001").body()).isEqualTo("v2: ELE-001");
            assertThat(get(app, "/api/v3/catalog/ELE-001").statusCode()).isEqualTo(400);   // nieznana wersja
            // PUŁAPKA: adres bez wersji w segmencie 1 — „health” zostaje odczytane jako wersja i nie jest nią → 400
            assertThat(get(app, "/api/health").statusCode()).isEqualTo(400);
        }
    }

    @Test
    @DisplayName("3. Wersja w PARAMETRZE TYPU MEDIÓW: Accept: application/json;v=2.0 (ustawione właściwością)")
    void mediaTypeParameterFromProperty() throws Exception {
        try (ConfigurableApplicationContext app = start(PlainVersionedApp.class,
                "spring.mvc.apiversion.use.media-type-parameter[application/json]=v",
                "spring.mvc.apiversion.default=1.0")) {
            assertThat(get(app, "/api/hello", "Accept", "application/json;v=2.0").body()).isEqualTo("\"v2\"");
            assertThat(get(app, "/api/hello", "Accept", "application/json").body()).isEqualTo("\"v1\"");
        }
    }

    @Test
    @DisplayName("4. Wersja WYMAGANA bez domyślnej: brak nagłówka → 400 (MissingApiVersionException)")
    void requiredVersionMissing() throws Exception {
        try (ConfigurableApplicationContext app = start(PlainVersionedApp.class,
                "spring.mvc.apiversion.use.header=API-Version",
                "spring.mvc.apiversion.required=true")) {
            assertThat(get(app, "/api/hello").statusCode()).isEqualTo(400);
            assertThat(get(app, "/api/hello", "API-Version", "1.0").body()).isEqualTo("\"v1\"");
        }
    }

    @Test
    @DisplayName("4. Wersja NIEwymagana i bez domyślnej: zapytanie bez wersji dostaje NAJNOWSZĄ wersję (pytanie 6)")
    void notRequiredNoDefaultGivesLatest() throws Exception {
        try (ConfigurableApplicationContext app = start(PlainVersionedApp.class,
                "spring.mvc.apiversion.use.header=API-Version",
                "spring.mvc.apiversion.required=false")) {
            assertThat(get(app, "/api/hello").body()).isEqualTo("\"v2\"");
        }
    }

    @Test
    @DisplayName("4. detect-supported=false: wersja jest w mapowaniu, ale nie na liście supported → 400")
    void detectSupportedOff() throws Exception {
        try (ConfigurableApplicationContext app = start(PlainVersionedApp.class,
                "spring.mvc.apiversion.use.header=API-Version",
                "spring.mvc.apiversion.default=1.0",
                "spring.mvc.apiversion.detect-supported=false")) {
            assertThat(get(app, "/api/hello", "API-Version", "1.0").statusCode()).isEqualTo(200);   // domyślna = wspierana
            assertThat(get(app, "/api/hello", "API-Version", "2.0").statusCode()).isEqualTo(400);
        }
    }

    @Test
    @DisplayName("5. PUŁAPKA: version w mapowaniu bez skonfigurowanego wersjonowania → aplikacja NIE startuje (pytanie 4)")
    void versionWithoutStrategyFailsAtStartup() {
        assertThatThrownBy(() -> start(PlainVersionedApp.class).close())
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("API version specified, but no ApiVersionStrategy configured");
    }

    /** Kontroler z dwiema wersjami tego samego adresu (zwraca tekst JSON "v1" albo "v2"). */
    @RestController
    static class HelloController {

        @GetMapping(path = "/api/hello", version = "1.0", produces = "application/json")
        String v1() {
            return "\"v1\"";
        }

        @GetMapping(path = "/api/hello", version = "2.0", produces = "application/json")
        String v2() {
            return "\"v2\"";
        }
    }

    /** Aplikacja ustawiana WŁAŚCIWOŚCIAMI (spring.mvc.apiversion.*) przekazanymi w teście. */
    @Configuration
    @EnableAutoConfiguration
    static class PlainVersionedApp {

        @Bean
        HelloController helloController() {
            return new HelloController();
        }
    }

    /** Kontroler „wersja w ścieżce”: {version} w wzorcu adresu, a i tak wybór metody robi atrybut version. */
    @RestController
    static class CatalogController {

        @GetMapping(path = "/api/{version}/catalog/{sku}", version = "1.0")
        String v1(@PathVariable String sku) {
            return "v1: " + sku;
        }

        @GetMapping(path = "/api/{version}/catalog/{sku}", version = "2.0")
        String v2(@PathVariable String sku) {
            return "v2: " + sku;
        }
    }

    /** Zwykły adres BEZ wersji w ścieżce — w aplikacji z usePathSegment(1) to pułapka. */
    @RestController
    static class HealthController {

        @GetMapping("/api/health")
        String health() {
            return "OK";
        }
    }

    /** Aplikacja ustawiana KODEM: WebMvcConfigurer.configureApiVersioning. */
    @Configuration
    @EnableAutoConfiguration
    static class PathSegmentApp implements WebMvcConfigurer {

        @Bean
        CatalogController catalogController() {
            return new CatalogController();
        }

        @Bean
        HealthController healthController() {
            return new HealthController();
        }

        @Override
        public void configureApiVersioning(ApiVersionConfigurer configurer) {
            configurer.usePathSegment(1);   // /api/{0}... — segment o indeksie 1 (liczymy od 0: api=0, v1=1)
        }
    }
}
