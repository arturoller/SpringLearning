package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy sekcji 1 i 2 lekcji Sec01Basics — aplikacja „SAM STARTER” (Sec01StarterOnlyApplication).
 *
 * <p>Tu nie ma {@code @SpringBootTest}: test sam uruchamia aplikację przez {@code SpringApplication} — tak jak ▶ przy
 * main — na losowym porcie ({@code --server.port=0}). Dzięki temu {@code OutputCaptureExtension} (przechwytywanie
 * wyjścia) widzi log startu z wygenerowanym hasłem, a zapytania idą przez prawdziwy serwer Tomcat.
 * {@code java.net.http.HttpClient} (klient HTTP z JDK) niczego nie ukrywa: nie podąża za przekierowaniami i nie
 * rzuca wyjątków przy 401 — widzimy surową odpowiedź.</p>
 */
@ExtendWith(OutputCaptureExtension.class)   // extend with = rozszerz o; przechwytuje System.out i log do CapturedOutput
class Sec01BasicsStarterOnlyTest {

    /** Wzór linii z logu: „Using generated security password: ” + UUID (losowy identyfikator). */
    static final Pattern PASSWORD_LINE = Pattern.compile("Using generated security password: ([0-9a-f-]{36})");

    final HttpClient http = HttpClient.newHttpClient();

    /** Uruchamia aplikację „sam starter” na losowym porcie z podanymi ustawieniami domyślnymi. */
    static ConfigurableApplicationContext startStarterOnly(Map<String, Object> defaults) {
        SpringApplication app = new SpringApplication(Sec01StarterOnlyApplication.class);
        app.setDefaultProperties(defaults);
        return app.run("--server.port=0");
    }

    static int port(ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    HttpResponse<String> get(int port, String path, String... headers) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (headers.length > 0) {
            request.headers(headers);   // pary: nazwa, wartość, nazwa, wartość...
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    /**
     * Hasło z OSTATNIEJ linii „Using generated security password” — przechwycone wyjście obejmuje też wcześniejsze
     * testy tej klasy (a każdy start aplikacji losuje nowe hasło).
     */
    static String lastGeneratedPassword(String log) {
        Matcher matcher = PASSWORD_LINE.matcher(log);
        String password = null;
        while (matcher.find()) {
            password = matcher.group(1);
        }
        assertThat(password).as("w logu powinna być linia z wygenerowanym hasłem").isNotNull();
        return password;
    }

    @Test
    @DisplayName("1. Sam starter: hasło w logu (WARN), każdy adres zablokowany — 401 z nagłówkiem WWW-Authenticate: Basic")
    void starterLocksEverything(CapturedOutput output) throws Exception {
        try (ConfigurableApplicationContext context = startStarterOnly(Map.of("spring.security.user.name", "user"))) {
            int port = port(context);

            // log startu: poziom WARN, klasa UserDetailsServiceAutoConfiguration, hasło = UUID
            assertThat(output.getAll())
                    .contains("WARN")
                    .contains("UserDetailsServiceAutoConfiguration")
                    .contains("This generated password is for development use only.");
            String generatedPassword = lastGeneratedPassword(output.getAll());

            // nawet „publiczna” lista produktów jest zablokowana — starter nie zna naszych zamiarów
            HttpResponse<String> anonymous = get(port, "/api/public/products");
            assertThat(anonymous.statusCode()).isEqualTo(401);
            assertThat(anonymous.headers().firstValue("WWW-Authenticate")).hasValue("Basic realm=\"Realm\", charset=\"UTF-8\"");
            assertThat(anonymous.body()).as("ciało 401 jest puste — strona /error też jest zablokowana").isEmpty();

            // użytkownik „user” + hasło z logu → wpuszczony
            HttpResponse<String> withPassword = get(port, "/api/public/products",
                    "Authorization", basic("user", generatedPassword));
            assertThat(withPassword.statusCode()).isEqualTo(200);
            assertThat(withPassword.body()).contains("Laptop Pro 14");

            // złe hasło → znowu 401
            assertThat(get(port, "/api/orders", "Authorization", basic("user", "zle-haslo")).statusCode()).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("1. Przeglądarka (Accept: text/html) dostaje 302 na /login, a pod /login jest gotowy formularz logowania")
    void browserIsRedirectedToLoginForm() throws Exception {
        try (ConfigurableApplicationContext context = startStarterOnly(Map.of("spring.security.user.name", "user"))) {
            int port = port(context);

            HttpResponse<String> fromBrowser = get(port, "/api/orders", "Accept", "text/html");
            assertThat(fromBrowser.statusCode()).isEqualTo(302);
            assertThat(fromBrowser.headers().firstValue("Location")).hasValue("http://localhost:" + port + "/login");

            HttpResponse<String> loginPage = get(port, "/login");
            assertThat(loginPage.statusCode()).isEqualTo(200);
            assertThat(loginPage.body()).contains("Please sign in").contains("name=\"username\"").contains("name=\"password\"");
        }
    }

    @Test
    @DisplayName("2. Domyślny łańcuch Boota: jeden SecurityFilterChain na każde zapytanie, 16 filtrów, w tym formularz i Basic")
    void defaultFilterChain() {
        try (ConfigurableApplicationContext context = startStarterOnly(Map.of("spring.security.user.name", "user"))) {
            FilterChainProxy proxy = context.getBean(FilterChainProxy.class);
            List<SecurityFilterChain> chains = proxy.getFilterChains();
            assertThat(chains).hasSize(1);
            List<String> filters = chains.get(0).getFilters().stream().map(f -> f.getClass().getSimpleName()).toList();
            assertThat(filters).containsExactly(
                    "DisableEncodeUrlFilter", "WebAsyncManagerIntegrationFilter", "SecurityContextHolderFilter",
                    "HeaderWriterFilter", "CsrfFilter", "LogoutFilter", "UsernamePasswordAuthenticationFilter",
                    "DefaultResourcesFilter", "DefaultLoginPageGeneratingFilter", "DefaultLogoutPageGeneratingFilter",
                    "BasicAuthenticationFilter", "RequestCacheAwareFilter", "SecurityContextHolderAwareRequestFilter",
                    "AnonymousAuthenticationFilter", "ExceptionTranslationFilter", "AuthorizationFilter");
        }
    }
}
