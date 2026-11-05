package s12_security.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcje 1–5 lekcji Sec04StatelessApi na wycinku {@code @WebMvcTest} (kontrolery + filtry bezpieczeństwa, bez serwera).
 * {@code @Import(Sec04SecurityConfig.class)}, bo wycinek NIE ładuje sam klasy {@code @Configuration} z łańcuchami filtrów
 * (Sec01Basics) — bez importu działałby domyślny łańcuch Boota.
 */
@WebMvcTest
@Import(Sec04SecurityConfig.class)
class Sec04StatelessApiTest {

    private static final String NEW_PRODUCT = """
            {"name": "Czekolada gorzka", "price": 12.99}
            """;

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 1: STATELESS — po udanym logowaniu Basic serwer nie zakłada sesji (getSession(false) == null)")
    void statelessCreatesNoSession() {
        MvcTestResult result = mvc.get().uri("/sec04/api/orders").with(httpBasic("anna", "haslo-testowe")).exchange();
        assertThat(result).hasStatusOk();
        assertThat(result.getMvcResult().getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("Sekcja 2: łańcuch z sesją — POST bez tokenu CSRF to 403, z tokenem 200, z fałszywym tokenem 403; GET tokenu nie wymaga")
    void csrfProtectsStateChangingRequests() {
        assertThat(mvc.post().uri("/sec04/web/email").param("email", "anna@sklep.example")
                .with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/sec04/web/email").param("email", "anna@sklep.example")
                .with(httpBasic("anna", "haslo-testowe")).with(csrf()))          // csrf() = dołącz poprawny token CSRF
                .hasStatusOk()
                .hasBodyTextEqualTo("Nowy e-mail użytkownika anna: anna@sklep.example");
        assertThat(mvc.post().uri("/sec04/web/email").param("email", "anna@sklep.example")
                .with(httpBasic("anna", "haslo-testowe")).with(csrf().useInvalidToken()))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec04/web/profile").with(httpBasic("anna", "haslo-testowe"))).hasStatusOk();
    }

    @Test
    @DisplayName("Sekcja 2: CSRF sprawdzany jest PRZED logowaniem — nawet anonim bez tokenu dostaje 403, a nie 401")
    void csrfCheckedBeforeAuthentication() {
        assertThat(mvc.post().uri("/sec04/web/email").param("email", "x@zly.example")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Sekcja 2: API z wyłączonym CSRF — POST z samym nagłówkiem Authorization przechodzi (201)")
    void apiWithoutCsrf() {
        assertThat(mvc.post().uri("/sec04/api/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT)
                .with(httpBasic("ewa", "haslo-testowe")))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Sekcja 3: preflight z dozwolonego pochodzenia BEZ nagłówka Authorization → 200 i nagłówki Access-Control-Allow-*")
    void preflightAllowed() {
        assertThat(mvc.options().uri("/sec04/api/orders")
                .header(HttpHeaders.ORIGIN, Sec04SecurityConfig.FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST")
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization")
                .hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }

    @Test
    @DisplayName("Sekcja 3: obce pochodzenie albo niedozwolona metoda w preflight → 403 \"Invalid CORS request\"")
    void preflightRejected() {
        assertThat(mvc.options().uri("/sec04/api/orders")
                .header(HttpHeaders.ORIGIN, "http://zly.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasBodyTextEqualTo("Invalid CORS request");
        assertThat(mvc.options().uri("/sec04/api/orders")
                .header(HttpHeaders.ORIGIN, Sec04SecurityConfig.FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Sekcja 3: właściwe żądanie z Origin — odpowiedź ma Access-Control-Allow-Origin, ale logowanie dalej obowiązuje")
    void actualCorsRequest() {
        assertThat(mvc.get().uri("/sec04/api/orders").header(HttpHeaders.ORIGIN, Sec04SecurityConfig.FRONTEND_ORIGIN)
                .with(httpBasic("anna", "haslo-testowe")))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
        assertThat(mvc.get().uri("/sec04/api/orders").header(HttpHeaders.ORIGIN, Sec04SecurityConfig.FRONTEND_ORIGIN))
                .hasStatus(HttpStatus.UNAUTHORIZED);            // CORS to nie logowanie: bez Authorization dalej 401
    }

    @Test
    @DisplayName("Sekcja 4: brak logowania i złe hasło → 401 jako ProblemDetail (application/problem+json) z WWW-Authenticate")
    void unauthorizedAsProblemDetail() {
        assertThat(mvc.get().uri("/sec04/api/orders"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"sklep\"")
                .bodyJson().isLenientlyEqualTo("""
                        {"status": 401, "title": "Wymagane logowanie", "instance": "/sec04/api/orders"}
                        """);
        assertThat(mvc.get().uri("/sec04/api/orders").with(httpBasic("anna", "zle-haslo")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.title").isEqualTo("Wymagane logowanie");
    }

    @Test
    @DisplayName("Sekcja 4: brak uprawnień → 403 jako ProblemDetail; łańcuch przeglądarki ma dalej domyślne 401 bez JSON-a")
    void forbiddenAsProblemDetail() {
        assertThat(mvc.get().uri("/sec04/api/admin/stats").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        {"status": 403, "title": "Brak uprawnień", "detail": "Twoje konto nie ma dostępu do tego zasobu."}
                        """);
        assertThat(mvc.get().uri("/sec04/web/profile"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\", charset=\"UTF-8\"")
                .hasBodyTextEqualTo("");
    }

    @Test
    @DisplayName("Sekcja 5: domyślne nagłówki bezpieczeństwa: nosniff, DENY, no-store; HSTS tylko dla HTTPS")
    void securityHeaders() {
        assertThat(mvc.get().uri("/sec04/api/products"))
                .hasStatusOk()
                .hasHeader("X-Content-Type-Options", "nosniff")
                .hasHeader("X-Frame-Options", "DENY")
                .hasHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
                .hasHeader("X-XSS-Protection", "0")
                .doesNotContainHeader("Strict-Transport-Security");
        assertThat(mvc.get().uri("/sec04/api/products").secure(true))     // secure(true) = udawaj żądanie przez HTTPS
                .hasStatusOk()
                .hasHeader("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains");
    }
}
