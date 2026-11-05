package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcje 2–5 lekcji Sec05Jwt: wydawanie tokenu i API z tokenem. {@code @SpringBootTest} + MockMvc, bo potrzebne są prawdziwe
 * beany: JwtEncoder, JwtDecoder (ten sam klucz), TokenService i oba łańcuchy filtrów. Token zdobywamy tak jak klient:
 * {@code POST /sec05/token} z hasłem.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sec05ResourceServerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder encoder;

    @Autowired
    InMemoryUserDetailsManager users;

    private String tokenFor(String username) {
        MvcTestResult result = mvc.post().uri("/sec05/token").with(httpBasic(username, "haslo-testowe")).exchange();
        assertThat(result).hasStatusOk();
        return JsonMapper.builder().build().readTree(result.getResponse().getContentAsByteArray())
                .get("access_token").asString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("Sekcja 2: POST /sec05/token z Basic → JSON {access_token, token_type: Bearer, expires_in: 900}; bez hasła 401 Basic")
    void issueToken() {
        assertThat(mvc.post().uri("/sec05/token").with(httpBasic("anna", "haslo-testowe")))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"token_type": "Bearer", "expires_in": 900}
                        """);
        assertThat(tokenFor("anna").split("\\.")).hasSize(3);
        assertThat(mvc.post().uri("/sec05/token").with(httpBasic("anna", "zle-haslo")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\", charset=\"UTF-8\"");
    }

    @Test
    @DisplayName("Sekcja 3: Authorization: Bearer <token> → 200; bez tokenu 401 z WWW-Authenticate: Bearer; samo Basic API nie przyjmuje")
    void bearerToken() {
        assertThat(mvc.get().uri("/sec05/orders").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor("anna"))))
                .hasStatusOk()
                .hasBodyTextEqualTo("Zamówienia użytkownika anna");
        assertThat(mvc.get().uri("/sec05/orders"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .headers().hasHeaderSatisfying(HttpHeaders.WWW_AUTHENTICATE, values -> assertThat(values.getFirst()).startsWith("Bearer"));
        assertThat(mvc.get().uri("/sec05/orders").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/sec05/products")).hasStatusOk();          // publiczne dalej bez tokenu
    }

    @Test
    @DisplayName("Sekcja 3: zmieniony token → 401 Bearer error=\"invalid_token\" … Invalid signature; śmieci → Malformed token")
    void invalidToken() {
        String token = tokenFor("anna");
        int signatureStart = token.lastIndexOf('.') + 1;
        char changed = token.charAt(signatureStart) == 'x' ? 'y' : 'x';                    // zmieniamy 1 znak podpisu
        String tampered = token.substring(0, signatureStart) + changed + token.substring(signatureStart + 1);
        assertThat(mvc.get().uri("/sec05/orders").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .headers().hasHeaderSatisfying(HttpHeaders.WWW_AUTHENTICATE, values -> assertThat(values.getFirst())
                        .startsWith("Bearer error=\"invalid_token\"")
                        .contains("Invalid signature"));
        assertThat(mvc.get().uri("/sec05/orders").header(HttpHeaders.AUTHORIZATION, bearer("abc.def.ghi")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .headers().hasHeaderSatisfying(HttpHeaders.WWW_AUTHENTICATE, values -> assertThat(values.getFirst())
                        .contains("Malformed token"));
    }

    @Test
    @DisplayName("Sekcja 3: token wystawiony 2 godziny temu (zegar cofnięty w TokenService) → 401 \"Jwt expired at …\"")
    void expiredToken() {
        TokenService twoHoursAgo = new TokenService(encoder, Clock.offset(Clock.systemUTC(), Duration.ofHours(-2)));
        String expired = twoHoursAgo.issue(new UsernamePasswordAuthenticationToken("anna", null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"))).accessToken();
        assertThat(mvc.get().uri("/sec05/orders").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .headers().hasHeaderSatisfying(HttpHeaders.WWW_AUTHENTICATE, values -> assertThat(values.getFirst())
                        .startsWith("Bearer error=\"invalid_token\"")
                        .contains("Jwt expired at"));
    }

    @Test
    @DisplayName("Sekcja 4: twierdzenie roles → ROLE_…; Security 7 dodaje FACTOR_BEARER; klientka w panelu admina 403 insufficient_scope")
    void rolesFromToken() {
        assertThat(mvc.get().uri("/sec05/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor("anna"))))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"name": "anna", "authorities": ["FACTOR_BEARER", "ROLE_CUSTOMER"], "rolesClaim": ["CUSTOMER"]}
                        """);
        assertThat(mvc.get().uri("/sec05/admin/stats").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor("anna"))))
                .hasStatus(HttpStatus.FORBIDDEN)
                .headers().hasHeaderSatisfying(HttpHeaders.WWW_AUTHENTICATE, values -> assertThat(values.getFirst())
                        .startsWith("Bearer error=\"insufficient_scope\""));
        assertThat(mvc.get().uri("/sec05/admin/stats").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor("admin"))))
                .hasStatusOk();
    }

    @Test
    @DisplayName("Sekcja 5: serwer nie pamięta tokenów — po usunięciu konta anna nie zaloguje się hasłem, ale jej token działa do exp")
    void tokenOutlivesAccount() {
        String token = tokenFor("anna");
        users.deleteUser("anna");
        try {
            assertThat(mvc.post().uri("/sec05/token").with(httpBasic("anna", "haslo-testowe")))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(mvc.get().uri("/sec05/orders").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .hasStatusOk();                                         // nikt nie pyta bazy — liczy się podpis i exp
        } finally {
            users.createUser(User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());
        }
    }
}
