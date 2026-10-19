package s12_security.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy sekcji 2 lekcji Sec02Authorization — kolejność reguł i literówki w rolach. Każdy test startuje małą aplikację
 * (kontroler StaffController + użytkownicy Sec02Users + CELOWO BŁĘDNY łańcuch) na losowym porcie.
 * Błędne łańcuchy żyją tylko tutaj, w testach — Sec02Application ich nie widzi.
 */
class Sec02AuthorizationRuleOrderTest {

    /** ZŁA kolejność: ogólna reguła /api/** przed szczegółową /api/hr/**. */
    @EnableAutoConfiguration
    @Import({StaffController.class, Sec02Users.class})
    static class GeneralRuleFirstApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/**").authenticated()                  // pasuje też do /api/hr/employees...
                    .requestMatchers("/api/hr/**").hasAnyRole("HR", "ADMIN")     // ...więc ta reguła nigdy nie zadziała
                    .anyRequest().denyAll());
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** anyRequest() na początku. */
    @EnableAutoConfiguration
    @Import({StaffController.class, Sec02Users.class})
    static class AnyRequestFirstApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
                    .requestMatchers("/api/hr/**").hasRole("HR"));
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** hasRole z prefiksem ROLE_. */
    @EnableAutoConfiguration
    @Import({StaffController.class, Sec02Users.class})
    static class RolePrefixApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/hr/**").hasRole("ROLE_HR")
                    .anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** hasAuthority zamiast hasRole. */
    @EnableAutoConfiguration
    @Import({StaffController.class, Sec02Users.class})
    static class AuthorityInsteadOfRoleApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/hr/**").hasAuthority("HR")   // uprawnienie „HR” — a kasia ma „ROLE_HR”
                    .anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    final HttpClient http = HttpClient.newHttpClient();

    static ConfigurableApplicationContext start(Class<?> app) {
        return new SpringApplication(app).run("--server.port=0");
    }

    int getAs(ConfigurableApplicationContext context, String user, String path) throws IOException, InterruptedException {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        String credentials = user + ":" + user + "-test-123";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    @DisplayName("2. PUŁAPKA: ogólna reguła przed szczegółową — piotr (USER) widzi pensje z /api/hr/employees")
    void generalRuleFirstOpensTheDoor() throws Exception {
        try (ConfigurableApplicationContext context = start(GeneralRuleFirstApp.class)) {
            assertThat(getAs(context, "piotr", "/api/hr/employees")).as("powinno być 403, a jest...").isEqualTo(200);
        }
    }

    @Test
    @DisplayName("2. ZNAJDŹ BŁĄD: anyRequest() przed requestMatchers — aplikacja nie startuje (IllegalStateException)")
    void anyRequestFirstFailsAtStartup() {
        assertThatThrownBy(() -> start(AnyRequestFirstApp.class))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can't configure requestMatchers after anyRequest");
    }

    @Test
    @DisplayName("1. ZNAJDŹ BŁĄD: hasRole(\"ROLE_HR\") — aplikacja nie startuje (IllegalArgumentException)")
    void rolePrefixFailsAtStartup() {
        assertThatThrownBy(() -> start(RolePrefixApp.class))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ROLE_HR should not start with ROLE_ since ROLE_ is automatically prepended when using "
                        + "hasAnyRole. Consider using hasAnyAuthority instead.");
    }

    @Test
    @DisplayName("1. PUŁAPKA: hasAuthority(\"HR\") zamiast hasRole(\"HR\") — kasia z rolą HR dostaje 403")
    void authorityInsteadOfRole() throws Exception {
        try (ConfigurableApplicationContext context = start(AuthorityInsteadOfRoleApp.class)) {
            assertThat(getAs(context, "kasia", "/api/hr/employees")).isEqualTo(403);
        }
    }
}
