package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 5 lekcji Rest06Cors: allowedOrigins("*") razem z allowCredentials(true). Każdy test startuje OSOBNĄ,
 * prawdziwą aplikację (losowy port) z celowo błędną konfiguracją — inaczej popsułaby ona aplikację lekcji.
 * Zwykła klasa testowa (bez {@code @SpringBootTest}) — zagnieżdżone {@code @Configuration} nie są konfiguracją testu.
 */
class Rest06CorsCredentialsTest {

    private static final String MESSAGE = "When allowCredentials is true, allowedOrigins cannot contain the special "
            + "value \"*\" since that cannot be set on the \"Access-Control-Allow-Origin\" response header. To allow "
            + "credentials to a set of origins, list them explicitly or consider using \"allowedOriginPatterns\" instead.";

    private static ConfigurableApplicationContext start(Class<?> config) {
        return new SpringApplicationBuilder(config).properties("server.port=0", "spring.main.banner-mode=off").run();
    }

    @Test
    @DisplayName("5. @CrossOrigin(origins = \"*\", allowCredentials = \"true\") → aplikacja NIE startuje (pytanie 4)")
    void annotationFailsAtStartup() {
        assertThatThrownBy(() -> start(AnnotationApp.class).close())
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE);
    }

    @Test
    @DisplayName("5. Globalnie allowedOrigins(\"*\") + allowCredentials(true) → start OK, ale KAŻDE zapytanie /api/** → 500")
    void globalConfigFailsAtRequestTime() throws Exception {
        try (ConfigurableApplicationContext app = start(GlobalApp.class)) {
            int port = app.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
            HttpClient client = HttpClient.newHttpClient();

            // nawet BEZ nagłówka Origin — zapytanie z curl albo z pliku .http też dostaje 500
            HttpResponse<String> withoutOrigin = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/g")).build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> withOrigin = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/g"))
                            .header("Origin", "http://localhost:3000").build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(withoutOrigin.statusCode()).isEqualTo(500);
            assertThat(withOrigin.statusCode()).isEqualTo(500);
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class AnnotationApp {

        @Bean
        AnnotationController annotationController() {
            return new AnnotationController();
        }
    }

    @RestController
    static class AnnotationController {

        @CrossOrigin(origins = "*", allowCredentials = "true")   // celowo błędnie
        @GetMapping("/api/a")
        String a() {
            return "a";
        }
    }

    @Configuration
    @EnableAutoConfiguration
    static class GlobalApp implements WebMvcConfigurer {

        @Bean
        GlobalController globalController() {
            return new GlobalController();
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**").allowedOrigins("*").allowCredentials(true);   // celowo błędnie
        }
    }

    @RestController
    static class GlobalController {

        @GetMapping("/api/g")
        String g() {
            return "g";
        }
    }
}
