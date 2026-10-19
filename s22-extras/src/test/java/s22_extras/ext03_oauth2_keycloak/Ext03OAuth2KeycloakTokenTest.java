package s22_extras.ext03_oauth2_keycloak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext03OAuth2Keycloak, sekcja 5b: PEŁNA ścieżka tokenu — nagłówek Authorization, dekoder (podpis, exp, iss)
 * i NASZ JwtAuthenticationConverter. Zamiast Keycloaka: para kluczy RSA generowana w teście (nic nie leży w repozytorium)
 * i tokeny o kształcie tokenów Keycloaka (realm_access, preferred_username, scope).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ext03")
@Import(Ext03OAuth2KeycloakTokenTest.TestKeys.class)
class Ext03OAuth2KeycloakTokenTest {

    /** Ten sam wystawca co w application-ext03.properties. */
    static final String ISSUER = "http://localhost:8180/realms/sklep";

    /**
     * Klucze TESTOWE i dekoder w roli „Keycloaka”. Bean JwtDecoder zastępuje dekoder Boota z issuer-uri
     * (autokonfiguracja ma {@code @ConditionalOnMissingBean}) — z tym samym sprawdzaniem wystawcy.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestKeys {

        @Bean
        KeyPair testKeyPair() throws NoSuchAlgorithmException {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }

        @Bean
        JwtDecoder jwtDecoder(KeyPair testKeyPair) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) testKeyPair.getPublic()).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));   // exp/nbf + iss, jak Boot
            return decoder;
        }

        @Bean
        JwtEncoder testJwtEncoder(KeyPair testKeyPair) {
            return NimbusJwtEncoder.withKeyPair((RSAPublicKey) testKeyPair.getPublic(),
                    (RSAPrivateKey) testKeyPair.getPrivate()).build();
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder encoder;

    /** Token w kształcie tokenu z Keycloaka 26 (por. sekcja 3 lekcji). Ważny 5 minut od „teraz”. */
    String keycloakLikeToken(String issuer, String username, List<String> realmRoles, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("ef5e0e7f-10fd-4236-a28a-6232b782e2c3")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(5, ChronoUnit.MINUTES))
                .claim("azp", "sklep-cli")
                .claim("realm_access", Map.of("roles", realmRoles))
                .claim("scope", "profile email")
                .claim("preferred_username", username)
                .claim("email", username + "@example.com")
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Test
    @DisplayName("4. Token anny → GET /api/me: nazwa z preferred_username, uprawnienia SCOPE_ + ROLE_ + FACTOR_BEARER (pytanie 8)")
    void annaThroughRealConverter() {
        String token = keycloakLikeToken(ISSUER, "anna", List.of("pracownik", "klient"), Instant.now());

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer " + token))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"name": "anna", "email": "anna@example.com",
                         "authorities": ["FACTOR_BEARER", "ROLE_KLIENT", "ROLE_PRACOWNIK", "SCOPE_email", "SCOPE_profile"]}""");
        assertThat(mvc.get().uri("/api/admin/orders").header("Authorization", "Bearer " + token)).hasStatusOk();
    }

    @Test
    @DisplayName("4/6. Token jana (tylko klient): własne zamówienia 200, wszystkie zamówienia 403 insufficient_scope")
    void janIsOnlyClient() {
        String token = keycloakLikeToken(ISSUER, "jan", List.of("klient"), Instant.now());

        assertThat(mvc.get().uri("/api/orders/my").header("Authorization", "Bearer " + token))
                .hasStatusOk().bodyJson().isStrictlyEqualTo("[\"ZAM-001 (jan)\"]");
        assertThat(mvc.get().uri("/api/admin/orders").header("Authorization", "Bearer " + token))
                .hasStatus(403)
                .headers().hasHeaderSatisfying("WWW-Authenticate", values -> assertThat(values.getFirst())
                        .startsWith("Bearer error=\"insufficient_scope\", error_description=\"The request requires higher"
                                + " privileges than provided by the access token.\""));
    }

    @Test
    @DisplayName("2. PUŁAPKA: inny wystawca (127.0.0.1 zamiast localhost) → 401 „The iss claim is not valid” (pytanie 4)")
    void wrongIssuer() {
        String token = keycloakLikeToken("http://127.0.0.1:8180/realms/sklep", "jan", List.of("klient"), Instant.now());

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer " + token))
                .hasStatus(401)
                .headers().hasHeaderSatisfying("WWW-Authenticate", values -> assertThat(values.getFirst())
                        .startsWith("Bearer error=\"invalid_token\", error_description=\"An error occurred while attempting"
                                + " to decode the Jwt: The iss claim is not valid\""));
    }

    @Test
    @DisplayName("6. Token przeterminowany (wystawiony w 2020 r.) → 401 invalid_token „Jwt expired at ...”")
    void expiredToken() {
        String token = keycloakLikeToken(ISSUER, "jan", List.of("klient"), Instant.parse("2020-01-01T10:00:00Z"));

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer " + token))
                .hasStatus(401)
                .headers().hasHeaderSatisfying("WWW-Authenticate", values -> assertThat(values.getFirst())
                        .contains("error=\"invalid_token\"")
                        .contains("Jwt expired at 2020-01-01T10:05:00Z"));
    }
}
