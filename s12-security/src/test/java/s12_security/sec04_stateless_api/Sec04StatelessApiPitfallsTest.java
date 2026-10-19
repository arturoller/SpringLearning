package s12_security.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.UnreachableFilterChainException;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy PUŁAPEK lekcji Sec04StatelessApi — każda na osobnej, małej aplikacji (kontroler lekcji + celowo niepełny
 * łańcuch) na losowym porcie.
 */
class Sec04StatelessApiPitfallsTest {

    static class TestUsers {
        @Bean
        UserDetailsService users() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("piotr").password("{noop}piotr-test-123").roles("USER").build());
        }
    }

    /** CSRF włączone, /error NIE jest publiczne. */
    @EnableAutoConfiguration
    @Import({WebController.class, TestUsers.class})
    static class ErrorPageProtectedApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** To samo, ale z .requestMatchers("/error").permitAll(). */
    @EnableAutoConfiguration
    @Import({WebController.class, TestUsers.class})
    static class ErrorPagePublicApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** CORS skonfigurowany TYLKO w Spring MVC (jak w s04_web_rest/Rest06Cors), łańcuch bez http.cors(...). */
    @EnableAutoConfiguration
    @Import({ApiController.class, TestUsers.class})
    static class MvcCorsWithoutSecurityCorsApp implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**").allowedOrigins("http://localhost:5173").allowedMethods("GET", "POST");
        }

        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            http.csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    /** Ta sama konfiguracja MVC, ale łańcuch z http.cors(withDefaults()) — Security użyje konfiguracji CORS z MVC. */
    @EnableAutoConfiguration
    @Import({ApiController.class, TestUsers.class})
    static class MvcCorsWithSecurityCorsApp implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**").allowedOrigins("http://localhost:5173").allowedMethods("GET", "POST");
        }

        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            http.csrf(AbstractHttpConfigurer::disable);
            http.cors(Customizer.withDefaults());
            return http.build();
        }
    }

    /** Dwa łańcuchy, ale łańcuch „na wszystko” jest PIERWSZY (Order 1), a ten dla /api/** — drugi. */
    @EnableAutoConfiguration
    @Import({ApiController.class, TestUsers.class})
    static class CatchAllChainFirstApp {
        @Bean
        @Order(1)
        SecurityFilterChain webChain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            http.formLogin(Customizer.withDefaults());
            return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain apiChain(HttpSecurity http) {
            http.securityMatcher("/api/**");
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    /** JSON-owy punkt wejścia TYLKO w exceptionHandling, httpBasic z ustawieniami domyślnymi, /error publiczne. */
    @EnableAutoConfiguration
    @Import({ApiController.class, TestUsers.class})
    static class EntryPointOnlyInExceptionHandlingApp {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated());
            http.httpBasic(Customizer.withDefaults());
            http.exceptionHandling(ex -> ex.authenticationEntryPoint(new ProblemDetailSecurityHandler()));
            http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }
    }

    final HttpClient http = HttpClient.newHttpClient();

    static ConfigurableApplicationContext start(Class<?> app) {
        return new SpringApplication(app).run("--server.port=0");
    }

    HttpResponse<String> send(ConfigurableApplicationContext context, String method, String path, String... headers)
            throws IOException, InterruptedException {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (headers.length > 0) {
            request.headers(headers);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("1. PUŁAPKA: łańcuch „na wszystko” przed łańcuchem /api/** — aplikacja nie startuje")
    void catchAllChainMustBeLast() {
        assertThatThrownBy(() -> start(CatchAllChainFirstApp.class))
                .rootCause()
                .isInstanceOf(UnreachableFilterChainException.class)
                .hasMessageStartingWith("A filter chain that matches any request [")
                .hasMessageEndingWith("] will never get invoked. Please use `HttpSecurity#securityMatcher` to ensure "
                        + "that there is only one filter chain configured for 'any request' and that the 'any request' "
                        + "filter chain is published last.");
    }

    @Test
    @DisplayName("3. PUŁAPKA: bez permitAll dla /error odmowa CSRF dla anonima kończy się 401 zamiast 403")
    void csrfDenialBecomes401WhenErrorPageIsProtected() throws Exception {
        try (ConfigurableApplicationContext context = start(ErrorPageProtectedApp.class)) {
            assertThat(send(context, "POST", "/web/newsletter").statusCode()).isEqualTo(401);
        }
        try (ConfigurableApplicationContext context = start(ErrorPagePublicApp.class)) {
            assertThat(send(context, "POST", "/web/newsletter").statusCode()).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("5. PUŁAPKA: entry point tylko w exceptionHandling — anonim dostaje JSON, ale ZŁE hasło już nie")
    void wrongPasswordUsesBasicEntryPoint() throws Exception {
        try (ConfigurableApplicationContext context = start(EntryPointOnlyInExceptionHandlingApp.class)) {
            HttpResponse<String> anonymous = send(context, "GET", "/api/orders");
            assertThat(anonymous.statusCode()).isEqualTo(401);
            assertThat(anonymous.headers().firstValue("Content-Type")).hasValue("application/problem+json");

            String wrong = "Basic " + Base64.getEncoder().encodeToString("piotr:zle-haslo".getBytes());
            HttpResponse<String> wrongPassword = send(context, "GET", "/api/orders", "Authorization", wrong);
            assertThat(wrongPassword.statusCode()).isEqualTo(401);
            assertThat(wrongPassword.headers().firstValue("WWW-Authenticate"))
                    .as("domyślny BasicAuthenticationEntryPoint, nie nasz").hasValue("Basic realm=\"Realm\", charset=\"UTF-8\"");
            assertThat(wrongPassword.headers().firstValue("Content-Type").orElse("brak"))
                    .doesNotContain("application/problem+json");
        }
    }

    @Test
    @DisplayName("4. PUŁAPKA: CORS tylko w Spring MVC, bez http.cors() — preflight dostaje 401; z http.cors() — 200")
    void corsMustBeEnabledInSecurity() throws Exception {
        String[] preflight = {"Origin", "http://localhost:5173", "Access-Control-Request-Method", "POST"};
        try (ConfigurableApplicationContext context = start(MvcCorsWithoutSecurityCorsApp.class)) {
            assertThat(send(context, "OPTIONS", "/api/orders", preflight).statusCode()).isEqualTo(401);
        }
        try (ConfigurableApplicationContext context = start(MvcCorsWithSecurityCorsApp.class)) {
            HttpResponse<String> response = send(context, "OPTIONS", "/api/orders", preflight);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
        }
    }
}
