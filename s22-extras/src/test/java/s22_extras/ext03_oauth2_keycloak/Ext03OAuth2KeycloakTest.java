package s22_extras.ext03_oauth2_keycloak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Testy lekcji Ext03OAuth2Keycloak z {@code jwt()} — bez Keycloaka. {@code @SpringBootTest} z profilem ext03 (prawdziwa
 * konfiguracja: issuer-uri, łańcuch filtrów lekcji) + MockMvcTester.
 * PAMIĘTAJ: jwt() pomija JwtDecoder i NASZ JwtAuthenticationConverter (sekcja 5) — pełną ścieżkę sprawdza
 * Ext03OAuth2KeycloakTokenTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ext03")
class Ext03OAuth2KeycloakTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    @DisplayName("2. Kontekst wstaje BEZ Keycloaka: JwtDecoder z issuer-uri jest leniwy (SupplierJwtDecoder)")
    void lazyDecoderFromIssuerUri() {
        assertThat(jwtDecoder).isInstanceOf(SupplierJwtDecoder.class);
    }

    @Test
    @DisplayName("6. Publiczny adres bez tokenu → 200; chroniony bez tokenu → 401 + WWW-Authenticate: Bearer")
    void publicAndUnauthenticated() {
        assertThat(mvc.get().uri("/api/public/products")).hasStatusOk();

        assertThat(mvc.get().uri("/api/me"))
                .hasStatus(401)
                .headers().hasValue("WWW-Authenticate",
                        "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\"");
    }

    @Test
    @DisplayName("5. jwt() z ROLE_KLIENT: własne zamówienia 200, zamówienia wszystkich 403 (insufficient_scope)")
    void clientRole() {
        var klient = jwt().authorities(new SimpleGrantedAuthority("ROLE_KLIENT"));

        assertThat(mvc.get().uri("/api/orders/my").with(klient)).hasStatusOk();
        assertThat(mvc.get().uri("/api/admin/orders").with(klient))
                .hasStatus(403)
                .headers().hasHeaderSatisfying("WWW-Authenticate", values ->
                        assertThat(values.getFirst()).startsWith("Bearer error=\"insufficient_scope\""));
    }

    @Test
    @DisplayName("5. PUŁAPKA: jwt() z claimem realm_access, ale bez authorities(...) → 403 — konwerter aplikacji pominięty")
    void jwtIgnoresApplicationConverter() {
        var pracownikBezUprawnien = jwt().jwt(j -> j.claim("realm_access", Map.of("roles", List.of("pracownik"))));

        assertThat(mvc.get().uri("/api/admin/orders").with(pracownikBezUprawnien)).hasStatus(403);   // pytanie 3
    }

    @Test
    @DisplayName("5. jwt().authorities(new KeycloakRealmRoleConverter()) — ten sam konwerter ról co aplikacja → 200")
    void jwtWithKeycloakConverter() {
        var pracownik = jwt()
                .jwt(j -> j.claim("realm_access", Map.of("roles", List.of("pracownik"))))
                .authorities(new KeycloakRealmRoleConverter());

        assertThat(mvc.get().uri("/api/admin/orders").with(pracownik)).hasStatusOk();
    }

    @Test
    @DisplayName("5. jwt(): nazwa użytkownika to „sub”, a nie preferred_username — setPrincipalClaimName też pominięty")
    void jwtPrincipalNameIsSubject() {
        var jan = jwt()
                .jwt(j -> j.subject("ef5e0e7f-10fd").claim("preferred_username", "jan").claim("email", "jan@example.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_KLIENT"));

        assertThat(mvc.get().uri("/api/me").with(jan))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"name": "ef5e0e7f-10fd", "email": "jan@example.com", "authorities": ["ROLE_KLIENT"]}""");
    }
}
