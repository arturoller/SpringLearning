package s12_security.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Sec04StatelessApi. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * {@code MockHttpServletRequest}/{@code MockHttpServletResponse} (atrapy zapytania i odpowiedzi ze Spring Test)
 * pozwalają sprawdzić konfigurację CORS i punkt wejścia bez serwera.
 */
@Tag("cwiczenie")
class Sec04StatelessApiExercisesTest {

    static class TestUsers {
        @Bean
        UserDetailsService users() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("piotr").password("{noop}piotr-test-123").roles("USER").build());
        }
    }

    @EnableAutoConfiguration
    @Import({ApiController.class, TestUsers.class})
    static class Exercise3App {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            return Sec04StatelessApi.exercise3(http);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 1: które metody HTTP wymagają tokenu CSRF")
    void exercise1() {
        assertThat(Sec04StatelessApi.exercise1("POST")).isTrue();
        assertThat(Sec04StatelessApi.exercise1("delete")).isTrue();
        assertThat(Sec04StatelessApi.exercise1("PATCH")).isTrue();
        assertThat(Sec04StatelessApi.exercise1("PUT")).isTrue();
        assertThat(Sec04StatelessApi.exercise1("GET")).isFalse();
        assertThat(Sec04StatelessApi.exercise1("head")).isFalse();
        assertThat(Sec04StatelessApi.exercise1("OPTIONS")).isFalse();
        assertThat(Sec04StatelessApi.exercise1("TRACE")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: konfiguracja CORS dla /api/** i https://sklep.example")
    void exercise2() {
        CorsConfigurationSource source = Sec04StatelessApi.exercise2();

        CorsConfiguration api = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/orders"));
        assertThat(api).isNotNull();
        assertThat(api.checkOrigin("https://sklep.example")).isEqualTo("https://sklep.example");
        assertThat(api.checkOrigin("http://zly.example")).as("obce pochodzenie").isNull();
        assertThat(api.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST");
        assertThat(api.getAllowedHeaders()).containsExactlyInAnyOrder("Authorization", "Content-Type");
        assertThat(api.getMaxAge()).isEqualTo(1800L);

        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("GET", "/web/profile"))).isNull();
    }

    @Test
    @DisplayName("Ćwiczenie 3: bezstanowy łańcuch — POST bez CSRF 201, 401 bez Set-Cookie")
    void exercise3() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplication(Exercise3App.class).run("--server.port=0")) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            HttpClient http = HttpClient.newHttpClient();
            String base = "http://localhost:" + port;

            HttpResponse<String> created = http.send(HttpRequest.newBuilder(URI.create(base + "/api/orders"))
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("piotr:piotr-test-123".getBytes()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"ELE-003\",\"quantity\":1}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(created.statusCode()).isEqualTo(201);

            HttpResponse<String> anonymous = http.send(HttpRequest.newBuilder(URI.create(base + "/api/orders")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(anonymous.statusCode()).isEqualTo(401);
            assertThat(anonymous.headers().firstValue("Set-Cookie")).as("bez sesji").isEmpty();

            HttpResponse<String> products = http.send(HttpRequest.newBuilder(URI.create(base + "/api/products")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(products.statusCode()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: punkt wejścia z odpowiedzią 401 w formacie ProblemDetail")
    void exercise4() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ExerciseJsonEntryPoint().commence(request, response, new InsufficientAuthenticationException("brak logowania"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        JsonNode json = JsonMapper.builder().build().readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(json.get("title").asString()).isEqualTo("Unauthorized");
        assertThat(json.get("detail").asString()).isEqualTo("Zaloguj się, żeby kontynuować.");
        assertThat(json.get("instance").asString()).isEqualTo("/api/orders");
    }
}
