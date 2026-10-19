package s22_extras.ext03_oauth2_keycloak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext03OAuth2Keycloak, sekcja 4: mapowanie ról Keycloaka — zwykły test jednostkowy na obiektach Jwt
 * (bez Springa, bez sieci, bez podpisów).
 */
class Ext03OAuth2KeycloakConverterTest {

    static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token-testowy")
                .header("alg", "RS256")
                .subject("ef5e0e7f-10fd-4236-a28a-6232b782e2c3")
                .issuedAt(Instant.parse("2026-10-19T10:00:00Z"))
                .expiresAt(Instant.parse("2026-10-19T10:05:00Z"));
        claims.forEach(builder::claim);
        return builder.build();
    }

    static List<String> names(Collection<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }

    @Test
    @DisplayName("4. realm_access.roles [pracownik, klient] → ROLE_KLIENT, ROLE_PRACOWNIK (wielkie litery, przedrostek ROLE_)")
    void realmRolesToAuthorities() {
        Jwt anna = token(Map.of("realm_access", Map.of("roles", List.of("pracownik", "klient"))));

        assertThat(names(new KeycloakRealmRoleConverter().convert(anna))).containsExactly("ROLE_KLIENT", "ROLE_PRACOWNIK");
    }

    @Test
    @DisplayName("4. Brak claimu realm_access albo roles nie jest listą → brak uprawnień (bez wyjątku)")
    void missingOrBrokenClaim() {
        assertThat(new KeycloakRealmRoleConverter().convert(token(Map.of("scope", "profile")))).isEmpty();
        assertThat(new KeycloakRealmRoleConverter().convert(token(Map.of("realm_access", Map.of("roles", "klient")))))
                .isEmpty();
    }

    @Test
    @DisplayName("4. PUŁAPKA: role KLIENTA (resource_access) nie są rolami dziedziny — konwerter ich nie widzi")
    void clientRolesAreNotRealmRoles() {
        Jwt token = token(Map.of("resource_access", Map.of("sklep-api", Map.of("roles", List.of("magazynier")))));

        assertThat(new KeycloakRealmRoleConverter().convert(token)).isEmpty();
    }

    @Test
    @DisplayName("4. Boot 4.1: wyrażenie [realm_access][roles].![toUpperCase()] + przedrostek ROLE_ — jak właściwości")
    void bootExpressionEquivalent() {
        // Dokładnie to robi Boot z authorities-claim-expressions i authority-prefix
        var converter = new ExpressionJwtGrantedAuthoritiesConverter(
                new SpelExpressionParser().parseExpression("[realm_access][roles].![toUpperCase()]"));
        converter.setAuthorityPrefix("ROLE_");
        Jwt anna = token(Map.of("realm_access", Map.of("roles", List.of("pracownik", "klient"))));

        assertThat(names(converter.convert(anna))).containsExactly("ROLE_KLIENT", "ROLE_PRACOWNIK");
        assertThat(converter.convert(token(Map.of("scope", "profile")))).as("brak claimu = brak uprawnień").isEmpty();
    }
}
