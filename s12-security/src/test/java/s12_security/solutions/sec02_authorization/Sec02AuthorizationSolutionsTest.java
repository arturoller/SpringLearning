package s12_security.solutions.sec02_authorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import s12_security.sec02_authorization.ProductController;
import s12_security.sec02_authorization.Sec02Users;
import s12_security.sec02_authorization.StaffController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Sec02AuthorizationExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
class Sec02AuthorizationSolutionsTest {

    /** Aplikacja do wzorca 3: kontrolery lekcji + pracownicy Sec02Users + łańcuch z rozwiązania. */
    @EnableAutoConfiguration
    @Import({ProductController.class, StaffController.class, Sec02Users.class})
    static class Solution3App {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) {
            return Sec02AuthorizationSolutions.solution3(http);
        }
    }

    @EnableMethodSecurity
    static class MethodSecurityOn {
    }

    final HttpClient http = HttpClient.newHttpClient();

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    static void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    /** Wysyła zapytanie jako użytkownik (null = anonim); hasło testowe = login + "-test-123". Zwraca status. */
    int send(ConfigurableApplicationContext context, String user, String method, String path)
            throws IOException, InterruptedException {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, method.equals("POST")
                        ? HttpRequest.BodyPublishers.ofString("{\"sku\":\"DOM-002\",\"name\":\"Lampka biurkowa\",\"price\":129.00}")
                        : HttpRequest.BodyPublishers.noBody());
        if (user != null) {
            String credentials = user + ":" + user + "-test-123";
            request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    @DisplayName("Wzorzec 1: nazwa roli → uprawnienie z prefiksem ROLE_")
    void solution1() {
        assertThat(Sec02AuthorizationSolutions.solution1("ADMIN")).isEqualTo("ROLE_ADMIN");
        assertThat(Sec02AuthorizationSolutions.solution1("HR")).isEqualTo("ROLE_HR");
        assertThatThrownBy(() -> Sec02AuthorizationSolutions.solution1("ROLE_ADMIN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sec02AuthorizationSolutions.solution1(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sec02AuthorizationSolutions.solution1(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: tomasz ma rolę SALES i uprawnienie report:read naraz")
    void solution2() {
        var tomasz = Sec02AuthorizationSolutions.solution2();
        assertThat(tomasz.getUsername()).isEqualTo("tomasz");
        assertThat(tomasz.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SALES", "report:read");
    }

    @Test
    @DisplayName("Wzorzec 3: reguły dla produktów i kadr, metody HTTP, reszta zabroniona")
    void solution3() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplication(Solution3App.class).run("--server.port=0")) {
            assertThat(send(context, null, "GET", "/api/products")).isEqualTo(200);
            assertThat(send(context, "piotr", "POST", "/api/products")).isEqualTo(403);
            assertThat(send(context, "anna", "POST", "/api/products")).isEqualTo(201);
            assertThat(send(context, "anna", "DELETE", "/api/products/DOM-002")).isEqualTo(204);
            assertThat(send(context, null, "DELETE", "/api/products/DOM-002")).isEqualTo(401);
            assertThat(send(context, "kasia", "GET", "/api/hr/employees")).isEqualTo(200);
            assertThat(send(context, "piotr", "GET", "/api/hr/employees")).isEqualTo(403);
            assertThat(send(context, "anna", "GET", "/api/stats")).as("reszta zabroniona").isEqualTo(403);
            assertThat(send(context, "anna", "PUT", "/api/products/DOM-002")).as("PUT bez reguły").isEqualTo(403);
        }
    }

    @Test
    @DisplayName("Wzorzec 4: @PreAuthorize — HR widzi każdą pensję, pracownik tylko swoją")
    void solution4() {
        new ApplicationContextRunner().withUserConfiguration(MethodSecurityOn.class, SolutionSalaryService.class)
                .run(context -> {
                    SolutionSalaryService service = context.getBean(SolutionSalaryService.class);
                    loginAs("kasia", "HR");
                    assertThat(service.salaryOf("anna")).isEqualTo(14500);
                    loginAs("piotr", "USER");
                    assertThat(service.salaryOf("piotr")).isEqualTo(9800);
                    assertThatThrownBy(() -> service.salaryOf("anna")).isInstanceOf(AuthorizationDeniedException.class);
                });
    }
}
