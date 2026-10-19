package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.FilterChainProxy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy sekcji 2–4 lekcji Sec01Basics — aplikacja z WŁASNYM łańcuchem (Sec01Application + Sec01SecurityConfig).
 *
 * <p>{@code @SpringBootTest(webEnvironment = RANDOM_PORT)} — cały kontekst i prawdziwy Tomcat na losowym porcie.
 * Dlaczego nie MockMvc? Bo chcemy zobaczyć też to, co robi serwer po odmowie: przekierowanie na stronę błędu
 * {@code /error} i jej ciało JSON. MockMvc strony {@code /error} nie renderuje (Sec06SecurityTesting).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sec01BasicsTest {

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> get(String path, String... headers) throws IOException, InterruptedException {
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
    @DisplayName("2. Tomcat zna jeden filtr DelegatingFilterProxy, który oddaje pracę beanowi springSecurityFilterChain (FilterChainProxy)")
    void filterChainArchitecture() {
        DelegatingFilterProxyRegistrationBean registration = context.getBean(DelegatingFilterProxyRegistrationBean.class);
        assertThat(registration.getFilterName()).isEqualTo("springSecurityFilterChain");
        assertThat(registration.getOrder()).as("domyślna kolejność filtra Security w Tomcacie").isEqualTo(-100);

        assertThat(context.getBean("springSecurityFilterChain")).isInstanceOf(FilterChainProxy.class);
        FilterChainProxy proxy = context.getBean(FilterChainProxy.class);
        assertThat(proxy.getFilterChains()).hasSize(1);

        // nasz łańcuch: 12 filtrów — bez formularza logowania (nie włączyliśmy formLogin)
        List<String> filters = proxy.getFilterChains().get(0).getFilters().stream()
                .map(f -> f.getClass().getSimpleName()).toList();
        assertThat(filters).containsExactly(
                "DisableEncodeUrlFilter", "WebAsyncManagerIntegrationFilter", "SecurityContextHolderFilter",
                "HeaderWriterFilter", "CsrfFilter", "LogoutFilter", "BasicAuthenticationFilter",
                "RequestCacheAwareFilter", "SecurityContextHolderAwareRequestFilter", "AnonymousAuthenticationFilter",
                "ExceptionTranslationFilter", "AuthorizationFilter");
        assertThat(filters).doesNotContain("UsernamePasswordAuthenticationFilter", "DefaultLoginPageGeneratingFilter");
    }

    @Test
    @DisplayName("3. permitAll: lista produktów bez logowania → 200; authenticated: zamówienia bez logowania → 401, z Basic → 200")
    void ownChainRules() throws Exception {
        HttpResponse<String> products = get("/api/public/products");
        assertThat(products.statusCode()).isEqualTo(200);
        assertThat(products.body()).contains("Kawa ziarnista 1kg");

        HttpResponse<String> ordersAnonymous = get("/api/orders");
        assertThat(ordersAnonymous.statusCode()).isEqualTo(401);
        assertThat(ordersAnonymous.headers().firstValue("WWW-Authenticate"))
                .hasValue("Basic realm=\"Realm\", charset=\"UTF-8\"");

        HttpResponse<String> ordersPiotr = get("/api/orders", "Authorization", basic("piotr", "piotr-test-123"));
        assertThat(ordersPiotr.statusCode()).isEqualTo(200);
        assertThat(ordersPiotr.body()).contains("\"user\":\"piotr\"");
    }

    @Test
    @DisplayName("3. Własny łańcuch bez formLogin: nie ma strony /login, a przeglądarka dostaje 401 zamiast 302")
    void noFormLoginInOwnChain() throws Exception {
        assertThat(get("/login").statusCode()).isEqualTo(401);
        assertThat(get("/api/orders", "Accept", "text/html").statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("4. 401 = nie wiem, kim jesteś; 403 = wiem, ale nie wolno ci (piotr bez roli ADMIN)")
    void unauthorizedVersusForbidden() throws Exception {
        HttpResponse<String> anonymous = get("/api/admin/report");
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.body()).contains("\"status\":401").contains("\"error\":\"Unauthorized\"");

        HttpResponse<String> piotr = get("/api/admin/report", "Authorization", basic("piotr", "piotr-test-123"));
        assertThat(piotr.statusCode()).isEqualTo(403);
        assertThat(piotr.headers().firstValue("WWW-Authenticate")).as("403 nie prosi o logowanie").isEmpty();
        assertThat(piotr.body()).contains("\"status\":403").contains("\"error\":\"Forbidden\"");

        HttpResponse<String> anna = get("/api/admin/report", "Authorization", basic("anna", "anna-test-123"));
        assertThat(anna.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("4. PUŁAPKA: złe hasło na PUBLICZNYM adresie → 401 (filtr Basic odrzuca zapytanie, zanim sprawdzi reguły)")
    void wrongPasswordOnPublicEndpoint() throws Exception {
        assertThat(get("/api/public/products", "Authorization", basic("anna", "zle-haslo")).statusCode())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("4. Nieistniejący adres: niezalogowany dostaje 401 (nie 404), zalogowany — 404")
    void unknownUrl() throws Exception {
        assertThat(get("/api/nie-ma-takiego").statusCode()).isEqualTo(401);
        assertThat(get("/api/nie-ma-takiego", "Authorization", basic("anna", "anna-test-123")).statusCode())
                .isEqualTo(404);
    }
}
