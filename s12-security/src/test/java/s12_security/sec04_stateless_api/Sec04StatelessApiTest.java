package s12_security.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Sec04StatelessApi. {@code RANDOM_PORT} + {@code java.net.http.HttpClient}, bo w tej lekcji liczą się
 * ciasteczka (sesja JSESSIONID), nagłówki CORS i prawdziwe przekierowania. Klient „przeglądarka” ma
 * {@code CookieManager} (menedżer ciasteczek) — zapamiętuje i odsyła ciasteczka jak przeglądarka; klient „program”
 * nie pamięta niczego.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sec04StatelessApiTest {

    @LocalServerPort
    int port;

    @Autowired
    FilterChainProxy filterChainProxy;

    final HttpClient program = HttpClient.newHttpClient();

    HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    static String basic(String user) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + user + "-test-123").getBytes());
    }

    static HttpResponse<String> send(HttpClient client, HttpRequest.Builder request) throws IOException, InterruptedException {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Loguje „przeglądarkę” formularzem: GET /login (token CSRF z ukrytego pola) → POST /login. */
    HttpClient loggedInBrowser(String user) throws IOException, InterruptedException {
        HttpClient browser = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> loginPage = send(browser, request("/login"));
        Matcher token = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(loginPage.body());
        assertThat(token.find()).as("formularz logowania ma ukryte pole _csrf").isTrue();
        HttpResponse<String> login = send(browser, request("/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "username=" + user + "&password=" + user + "-test-123&_csrf=" + token.group(1))));
        assertThat(login.statusCode()).as("udane logowanie formularzem → przekierowanie").isEqualTo(302);
        return browser;
    }

    static List<String> filterNames(SecurityFilterChain chain) {
        return chain.getFilters().stream().map(f -> f.getClass().getSimpleName()).toList();
    }

    @Test
    @DisplayName("1. Dwa łańcuchy: pierwszy tylko dla /api/** (bez CSRF), drugi na resztę (CSRF, formularz); oba z CORS")
    void twoFilterChains() {
        List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
        assertThat(chains).hasSize(2);

        List<String> api = filterNames(chains.get(0));
        assertThat(api).contains("CorsFilter", "BasicAuthenticationFilter").doesNotContain("CsrfFilter");

        List<String> web = filterNames(chains.get(1));
        assertThat(web).contains("CsrfFilter", "UsernamePasswordAuthenticationFilter");
        // w webChain NIE ma http.cors(...), a CorsFilter jest — Security dokłada go sam, bo istnieje bean
        // UrlBasedCorsConfigurationSource
        assertThat(web).contains("CorsFilter");
    }

    @Test
    @DisplayName("2. STATELESS: API nie wysyła ciasteczka sesji ani przy 401, ani przy 200; łańcuch web — wysyła")
    void statelessApiCreatesNoSession() throws Exception {
        HttpResponse<String> anonymous = send(program, request("/api/orders"));
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.headers().firstValue("Set-Cookie")).isEmpty();

        HttpResponse<String> piotr = send(program, request("/api/orders").header("Authorization", basic("piotr")));
        assertThat(piotr.statusCode()).isEqualTo(200);
        assertThat(piotr.headers().firstValue("Set-Cookie")).isEmpty();

        HttpResponse<String> web = send(program, request("/web/profile"));
        assertThat(web.statusCode()).isEqualTo(302);
        assertThat(web.headers().firstValue("Set-Cookie")).hasValueSatisfying(c -> assertThat(c).startsWith("JSESSIONID="));
    }

    @Test
    @DisplayName("2. STATELESS: ciasteczko sesji z logowania formularzem NIE loguje do /api/** — 401")
    void statelessApiIgnoresSession() throws Exception {
        HttpClient browser = loggedInBrowser("piotr");
        assertThat(send(browser, request("/web/profile")).body()).isEqualTo("Profil: piotr");
        assertThat(send(browser, request("/api/orders")).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("3. CSRF wyłączone w API: POST z Basic bez tokenu → 201")
    void apiPostWithoutCsrfToken() throws Exception {
        HttpResponse<String> created = send(program, request("/api/orders")
                .header("Authorization", basic("piotr"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"ELE-003\",\"quantity\":2}")));
        assertThat(created.statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("3. CSRF w łańcuchu web: POST bez tokenu → 403, z tokenem w nagłówku X-CSRF-TOKEN → 200")
    void webPostNeedsCsrfToken() throws Exception {
        HttpClient browser = loggedInBrowser("piotr");

        assertThat(send(browser, request("/web/newsletter").POST(HttpRequest.BodyPublishers.noBody())).statusCode())
                .isEqualTo(403);

        String csrfJson = send(browser, request("/web/csrf")).body();
        String token = csrfJson.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        HttpResponse<String> withToken = send(browser, request("/web/newsletter")
                .header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(withToken.statusCode()).isEqualTo(200);
        assertThat(withToken.body()).isEqualTo("Zapisano na newsletter: piotr");
    }

    @Test
    @DisplayName("3. POST /login bez tokenu CSRF → 403; token sprzed logowania po zalogowaniu jest już nieważny")
    void loginFormNeedsCsrfAndTokenRotates() throws Exception {
        HttpClient browser = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> noToken = send(browser, request("/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=piotr&password=piotr-test-123")));
        assertThat(noToken.statusCode()).isEqualTo(403);

        HttpResponse<String> loginPage = send(browser, request("/login"));
        Matcher matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(loginPage.body());
        assertThat(matcher.find()).isTrue();
        String tokenBeforeLogin = matcher.group(1);
        send(browser, request("/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "username=piotr&password=piotr-test-123&_csrf=" + tokenBeforeLogin)));

        HttpResponse<String> oldToken = send(browser, request("/web/newsletter")
                .header("X-CSRF-TOKEN", tokenBeforeLogin)
                .POST(HttpRequest.BodyPublishers.noBody()));
        assertThat(oldToken.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("4. CORS: preflight z http://localhost:5173 → 200 i nagłówki Access-Control-*; z obcego pochodzenia → 403")
    void corsPreflight() throws Exception {
        HttpResponse<String> allowed = send(program, request("/api/orders")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"));
        assertThat(allowed.statusCode()).as("preflight nie ma loginu, a i tak przechodzi").isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Methods")).hasValue("GET,POST,PUT,DELETE");
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Headers")).hasValue("authorization, content-type");
        assertThat(allowed.headers().firstValue("Access-Control-Max-Age")).hasValue("3600");

        HttpResponse<String> foreign = send(program, request("/api/orders")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://zly.example")
                .header("Access-Control-Request-Method", "POST"));
        assertThat(foreign.statusCode()).isEqualTo(403);
        assertThat(foreign.body()).isEqualTo("Invalid CORS request");

        HttpResponse<String> simpleGet = send(program, request("/api/products").header("Origin", "http://localhost:5173"));
        assertThat(simpleGet.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
    }

    @Test
    @DisplayName("5. 401 i 403 w API jako ProblemDetail (application/problem+json); złe hasło też w JSON")
    void problemDetailErrors() throws Exception {
        HttpResponse<String> anonymous = send(program, request("/api/orders"));
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(anonymous.headers().firstValue("WWW-Authenticate")).hasValue("Basic realm=\"sklep-api\"");
        assertThat(anonymous.body()).isEqualTo(
                "{\"detail\":\"Brak lub złe dane logowania.\",\"instance\":\"/api/orders\",\"status\":401,\"title\":\"Unauthorized\"}");

        HttpResponse<String> wrongPassword = send(program, request("/api/orders")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("anna:zle".getBytes())));
        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.headers().firstValue("Content-Type")).hasValue("application/problem+json");

        HttpResponse<String> forbidden = send(program, request("/api/admin/report").header("Authorization", basic("piotr")));
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(forbidden.body()).isEqualTo(
                "{\"detail\":\"Nie masz uprawnień do tego zasobu.\",\"instance\":\"/api/admin/report\",\"status\":403,\"title\":\"Forbidden\"}");
    }
}
