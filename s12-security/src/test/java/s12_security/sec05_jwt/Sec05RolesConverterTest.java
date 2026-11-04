package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Sec05Jwt: z jakich twierdzeń powstają uprawnienia — test jednostkowy konwertera na ręcznie zbudowanym
 * obiekcie {@link Jwt} (bez podpisu, bez Springa).
 */
class Sec05RolesConverterTest {

    private static final Jwt JWT = Jwt.withTokenValue("token-testowy")
            .header("alg", "HS256")
            .subject("ewa")
            .issuedAt(Instant.parse("2026-11-04T10:00:00Z"))
            .expiresAt(Instant.parse("2026-11-04T10:15:00Z"))
            .claim("scope", "orders:read products:write")      // scope = zakres — standard OAuth 2, napisy rozdzielone spacją
            .claim("roles", List.of("EMPLOYEE"))
            .build();

    private static List<String> authorities(JwtAuthenticationConverter converter) {
        return converter.convert(JWT).getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }

    @Test
    @DisplayName("Sekcja 4: domyślny konwerter czyta TYLKO scope/scp → SCOPE_…; twierdzenie roles ignoruje")
    void defaultConverterUsesScope() {
        assertThat(authorities(new JwtAuthenticationConverter()))
                .containsExactly("FACTOR_BEARER", "SCOPE_orders:read", "SCOPE_products:write");
    }

    @Test
    @DisplayName("Sekcja 4: konwerter lekcji czyta roles → ROLE_EMPLOYEE (hasRole(\"EMPLOYEE\") zadziała), scope pomija")
    void lessonConverterUsesRoles() {
        assertThat(authorities(new Sec05SecurityConfig().rolesConverter()))
                .containsExactly("FACTOR_BEARER", "ROLE_EMPLOYEE");
    }
}
