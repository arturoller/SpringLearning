package s22_extras.solutions.ext03_oauth2_keycloak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ExpressionJwtGrantedAuthoritiesConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Ext03OAuth2KeycloakExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ext03OAuth2KeycloakSolutionsTest {

    static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token-testowy").header("alg", "RS256")
                .subject("ef5e0e7f-10fd-4236-a28a-6232b782e2c3")
                .issuedAt(Instant.parse("2026-10-19T10:00:00Z")).expiresAt(Instant.parse("2026-10-19T10:05:00Z"));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    @DisplayName("Wzorzec 1: nazwa do wyświetlenia — name, potem preferred_username, potem sub")
    void solution1() {
        assertThat(Ext03OAuth2KeycloakSolutions.solution1(token(Map.of("name", "Anna Nowak", "preferred_username", "anna"))))
                .isEqualTo("Anna Nowak");
        assertThat(Ext03OAuth2KeycloakSolutions.solution1(token(Map.of("name", " ", "preferred_username", "anna"))))
                .isEqualTo("anna");
        assertThat(Ext03OAuth2KeycloakSolutions.solution1(token(Map.of("preferred_username", "jan")))).isEqualTo("jan");
        assertThat(Ext03OAuth2KeycloakSolutions.solution1(token(Map.of("scope", "profile"))))
                .isEqualTo("ef5e0e7f-10fd-4236-a28a-6232b782e2c3");
    }

    @Test
    @DisplayName("Wzorzec 2: walidator odbiorcy — aud zawiera sklep-api albo azp = sklep-cli")
    void solution2() {
        OAuth2TokenValidator<Jwt> validator = Ext03OAuth2KeycloakSolutions.solution2();

        assertThat(validator.validate(token(Map.of("aud", List.of("account", "sklep-api")))).hasErrors()).isFalse();
        assertThat(validator.validate(token(Map.of("azp", "sklep-cli"))).hasErrors()).isFalse();

        OAuth2TokenValidatorResult foreign = validator.validate(token(Map.of("aud", List.of("account"), "azp", "inna-aplikacja")));
        assertThat(foreign.hasErrors()).isTrue();
        assertThat(foreign.getErrors()).singleElement().satisfies(error -> {
            assertThat(error.getErrorCode()).isEqualTo("invalid_token");
            assertThat(error.getDescription()).isEqualTo("Token nie jest przeznaczony dla API sklepu");
        });
        assertThat(validator.validate(token(Map.of("scope", "profile"))).hasErrors()).as("brak aud i azp").isTrue();
    }

    @Test
    @DisplayName("Wzorzec 3: wyrażenie SpEL dla ról klienta sklep-api (jak authorities-claim-expressions w Boot 4.1)")
    void solution3() {
        var converter = new ExpressionJwtGrantedAuthoritiesConverter(
                new SpelExpressionParser().parseExpression(Ext03OAuth2KeycloakSolutions.solution3()));
        converter.setAuthorityPrefix("ROLE_");
        Jwt magazynier = token(Map.of(
                "realm_access", Map.of("roles", List.of("klient")),
                "resource_access", Map.of("sklep-api", Map.of("roles", List.of("magazynier", "kierownik")))));

        assertThat(converter.convert(magazynier)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_MAGAZYNIER", "ROLE_KIEROWNIK");
        assertThat(converter.convert(token(Map.of("realm_access", Map.of("roles", List.of("klient"))))))
                .as("bez ról klienta sklep-api — brak uprawnień").isEmpty();
    }
}
