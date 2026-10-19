package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Sec01Basics. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Ćwiczenia 2 i 3: test startuje małą aplikację (kontroler ShopController + Twój łańcuch + dwóch użytkowników)
 * na losowym porcie i wysyła prawdziwe zapytania HTTP. Dopóki metoda rzuca „TODO”, aplikacja nie wystartuje.
 */
@Tag("cwiczenie")
class Sec01BasicsExercisesTest {

    /** Użytkownicy ćwiczeń — ci sami co w Sec01SecurityConfig. */
    static class TestUsers {
        @Bean
        UserDetailsService users() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("anna").password("{noop}anna-test-123").roles("ADMIN").build(),
                    User.withUsername("piotr").password("{noop}piotr-test-123").roles("USER").build());
        }
    }

    @EnableAutoConfiguration
    @Import({ShopController.class, TestUsers.class})
    static class Exercise2App {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            return Sec01Basics.exercise2(http);
        }
    }

    @EnableAutoConfiguration
    @Import({ShopController.class, TestUsers.class})
    static class Exercise3App {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            return Sec01Basics.exercise3(http);
        }
    }

    final HttpClient http = HttpClient.newHttpClient();

    static ConfigurableApplicationContext start(Class<?> app) {
        return new SpringApplication(app).run("--server.port=0");
    }

    HttpResponse<String> get(ConfigurableApplicationContext context, String path, String... headers)
            throws IOException, InterruptedException {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    @Test
    @DisplayName("Ćwiczenie 1: nagłówek Authorization dla HTTP Basic (Base64 z UTF-8)")
    void exercise1() {
        assertThat(Sec01Basics.exercise1("anna", "anna-test-123")).isEqualTo("Basic YW5uYTphbm5hLXRlc3QtMTIz");
        assertThat(Sec01Basics.exercise1("zofia", "hasło")).isEqualTo("Basic em9maWE6aGFzxYJv");
    }

    @Test
    @DisplayName("Ćwiczenie 2: publiczny katalog i /error, reszta tylko dla zalogowanych (HTTP Basic)")
    void exercise2() throws Exception {
        try (ConfigurableApplicationContext context = start(Exercise2App.class)) {
            assertThat(get(context, "/api/public/products").statusCode()).isEqualTo(200);
            HttpResponse<String> anonymous = get(context, "/api/orders");
            assertThat(anonymous.statusCode()).isEqualTo(401);
            assertThat(anonymous.body()).as("/error musi być publiczne — wtedy 401 ma ciało JSON").contains("\"status\":401");
            assertThat(get(context, "/api/orders", "Authorization", basic("piotr", "piotr-test-123")).statusCode())
                    .isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: formularz i Basic naraz, wyjątki dla katalogu i raportu administratora")
    void exercise3() throws Exception {
        try (ConfigurableApplicationContext context = start(Exercise3App.class)) {
            HttpResponse<String> browser = get(context, "/api/orders", "Accept", "text/html");
            assertThat(browser.statusCode()).isEqualTo(302);
            assertThat(browser.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).endsWith("/login"));

            HttpResponse<String> program = get(context, "/api/orders");
            assertThat(program.statusCode()).isEqualTo(401);
            assertThat(program.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(h -> assertThat(h).startsWith("Basic"));

            assertThat(get(context, "/login").statusCode()).isEqualTo(200);
            assertThat(get(context, "/api/public/products").statusCode()).isEqualTo(200);
            assertThat(get(context, "/api/admin/report", "Authorization", basic("piotr", "piotr-test-123")).statusCode())
                    .isEqualTo(403);
            assertThat(get(context, "/api/admin/report", "Authorization", basic("anna", "anna-test-123")).statusCode())
                    .isEqualTo(200);
        }
    }
}
