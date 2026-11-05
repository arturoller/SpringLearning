package s12_security.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 1 i 2 lekcji Sec04StatelessApi: sesja kontra API bez sesji — na PRAWDZIWYM serwerze (RANDOM_PORT), bo tylko
 * Tomcat wysyła prawdziwy nagłówek {@code Set-Cookie: JSESSIONID=…}. Klient (RestClient) zachowuje się jak bardzo prosta
 * przeglądarka: ciasteczko odsyłamy ręcznie nagłówkiem {@code Cookie} — przeglądarka robi to SAMA.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sec04SessionTest {

    private static final Pattern CSRF_FIELD = Pattern.compile("name=\"_csrf\" type=\"hidden\" value=\"([^\"]+)\"");

    @LocalServerPort
    int port;

    private record Reply(int status, HttpHeaders headers, String body) {

        /** Samo {@code JSESSIONID=…} z nagłówka Set-Cookie (bez atrybutów Path, HttpOnly). */
        String sessionCookie() {
            return headers.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                    .filter(cookie -> cookie.startsWith("JSESSIONID="))
                    .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                    .findFirst().orElse(null);
        }
    }

    private Reply call(HttpMethod method, String uri, Consumer<HttpHeaders> headers, String formBody) {
        RestClient.RequestBodySpec request = RestClient.create("http://localhost:" + port).method(method).uri(uri)
                .headers(headers);
        if (formBody != null) {
            request.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formBody);
        }
        return request.exchange((req, response) -> new Reply(response.getStatusCode().value(), response.getHeaders(),
                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    /** Logowanie formularzem jak w przeglądarce: GET /login (ciasteczko + token CSRF z ukrytego pola), potem POST /login. */
    private String loginWithForm(String username) {
        Reply loginPage = call(HttpMethod.GET, "/login", headers -> { }, null);
        Matcher csrf = CSRF_FIELD.matcher(loginPage.body());
        assertThat(csrf.find()).as("formularz logowania ma ukryte pole _csrf").isTrue();
        Reply login = call(HttpMethod.POST, "/login", headers -> headers.add(HttpHeaders.COOKIE, loginPage.sessionCookie()),
                "username=" + username + "&password=haslo-testowe&_csrf=" + csrf.group(1));
        assertThat(login.status()).as("udane logowanie przekierowuje (302)").isEqualTo(302);
        return login.sessionCookie();    // NOWY identyfikator sesji — Spring zmienia go po zalogowaniu (session fixation)
    }

    @Test
    @DisplayName("Sekcja 1: API (STATELESS) — ani 200, ani 401 nie ustawia ciasteczka; bez nagłówka Authorization serwer Cię nie zna")
    void statelessApiSetsNoCookie() {
        Reply ok = call(HttpMethod.GET, "/sec04/api/orders", headers -> headers.setBasicAuth("anna", "haslo-testowe"), null);
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.body()).isEqualTo("Zamówienia użytkownika anna");
        assertThat(ok.headers().get(HttpHeaders.SET_COOKIE)).isNull();

        Reply anonymous = call(HttpMethod.GET, "/sec04/api/orders", headers -> { }, null);
        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.headers().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("Sekcja 1: łańcuch z sesją — już 401 zakłada sesję (JSESSIONID), bo Spring zapamiętuje adres na po zalogowaniu")
    void defaultChainCreatesSessionEvenOn401() {
        Reply anonymous = call(HttpMethod.GET, "/sec04/web/profile", headers -> { }, null);
        assertThat(anonymous.status()).isEqualTo(401);
        assertThat(anonymous.sessionCookie()).startsWith("JSESSIONID=");
    }

    @Test
    @DisplayName("Sekcja 1: po logowaniu formularzem samo ciasteczko wystarcza — a API to samo ciasteczko ignoruje (401)")
    void sessionCookieIsEnoughForBrowserChain() {
        String cookie = loginWithForm("anna");
        Reply profile = call(HttpMethod.GET, "/sec04/web/profile", headers -> headers.add(HttpHeaders.COOKIE, cookie), null);
        assertThat(profile.status()).isEqualTo(200);
        assertThat(profile.body()).isEqualTo("Profil: anna");

        Reply api = call(HttpMethod.GET, "/sec04/api/orders", headers -> headers.add(HttpHeaders.COOKIE, cookie), null);
        assertThat(api.status()).as("STATELESS: API nie czyta sesji").isEqualTo(401);
    }

    @Test
    @DisplayName("Sekcja 1: HTTP Basic w Security 7 nie zapisuje logowania w sesji — 200 bez Set-Cookie także w łańcuchu z sesją")
    void basicDoesNotCreateSession() {
        Reply profile = call(HttpMethod.GET, "/sec04/web/profile", headers -> headers.setBasicAuth("anna", "haslo-testowe"),
                null);
        assertThat(profile.status()).isEqualTo(200);
        assertThat(profile.headers().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("Sekcja 2: atak CSRF — żądanie z ważnym ciasteczkiem sesji, ale bez tokenu CSRF → 403 (zmiana e-maila się nie udaje)")
    void csrfAttackIsBlocked() {
        String cookie = loginWithForm("anna");
        // Tak wygląda żądanie wysłane przez obcą stronę: przeglądarka SAMA dołącza ciasteczko anny, ale tokenu nikt obcy nie zna.
        Reply attack = call(HttpMethod.POST, "/sec04/web/email", headers -> headers.add(HttpHeaders.COOKIE, cookie),
                "email=napastnik@zly.example");
        assertThat(attack.status()).isEqualTo(403);
        assertThat(attack.body()).doesNotContain("napastnik");
    }
}
