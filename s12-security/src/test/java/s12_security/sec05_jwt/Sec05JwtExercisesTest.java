package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ 1–4 lekcji Sec05Jwt. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Zwykłe testy jednostkowe,
 * bez Springa; zegary zatrzymane ({@code Clock.fixed}), klucze losowe.
 */
@Tag("cwiczenie")
class Sec05JwtExercisesTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    /** Gotowy token (nagłówek {"alg":"HS256"}, dane {"sub":"anna","roles":["CUSTOMER"]}, podpis zmyślony — TYLKO DO NAUKI). */
    private static final String SAMPLE = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbm5hIiwicm9sZXMiOlsiQ1VTVE9NRVIiXX0.podpis-testowy";

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("t").header("alg", "HS256").claims(map -> map.putAll(claims)).build();
    }

    private static String token(SecretKey key, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("anna").issuedAt(NOW).expiresAt(expiresAt).build();
        return NimbusJwtEncoder.withSecretKey(key).build()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    @DisplayName("Ćwiczenie 1: payloadJson dekoduje środkową część tokenu; token z inną liczbą części → IllegalArgumentException")
    void exercise1() {
        assertThat(JwtParts.payloadJson(SAMPLE)).isEqualTo("{\"sub\":\"anna\",\"roles\":[\"CUSTOMER\"]}");
        assertThatThrownBy(() -> JwtParts.payloadJson("tylko.dwie")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: claimsFor — iss sklep-cw, sub, iat = now, exp = now + 10 min, roles")
    void exercise2() {
        JwtClaimsSet claims = ExerciseClaims.claimsFor("ewa", List.of("EMPLOYEE"), NOW);
        assertThat(claims.getClaimAsString("iss")).isEqualTo("sklep-cw");
        assertThat(claims.getSubject()).isEqualTo("ewa");
        assertThat(claims.getIssuedAt()).isEqualTo(NOW);
        assertThat(claims.getExpiresAt()).isEqualTo(Instant.parse("2026-11-04T10:10:00Z"));
        assertThat(claims.getClaimAsStringList("roles")).containsExactly("EMPLOYEE");
    }

    @Test
    @DisplayName("Ćwiczenie 3: roles → ROLE_…; brak twierdzenia roles → pusta lista")
    void exercise3() {
        ExerciseRolesConverter converter = new ExerciseRolesConverter();
        assertThat(converter.convert(jwtWithClaims(Map.of("roles", List.of("ADMIN", "EMPLOYEE")))))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EMPLOYEE");
        assertThat(converter.convert(jwtWithClaims(Map.of("sub", "anna")))).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 4: dekoder bez tolerancji — sekundę przed exp token ważny, 30 s po exp już nie; obcy klucz odrzucony")
    void exercise4() throws Exception {
        SecretKey key = KeyGenerator.getInstance("HmacSHA256").generateKey();     // TYLKO DO NAUKI
        String token = token(key, NOW.plus(Duration.ofMinutes(10)));
        Clock almostExpired = Clock.fixed(NOW.plus(Duration.ofSeconds(599)), ZoneOffset.UTC);
        Clock justExpired = Clock.fixed(NOW.plus(Duration.ofSeconds(630)), ZoneOffset.UTC);

        assertThat(ExerciseJwtConfig.decoder(key, almostExpired).decode(token).getSubject()).isEqualTo("anna");
        JwtDecoder strict = ExerciseJwtConfig.decoder(key, justExpired);
        assertThatThrownBy(() -> strict.decode(token)).isInstanceOf(JwtException.class).hasMessageContaining("expired");
        JwtDecoder otherKey = ExerciseJwtConfig.decoder(KeyGenerator.getInstance("HmacSHA256").generateKey(), almostExpired);
        assertThatThrownBy(() -> otherKey.decode(token)).hasMessageContaining("Invalid signature");
    }
}
