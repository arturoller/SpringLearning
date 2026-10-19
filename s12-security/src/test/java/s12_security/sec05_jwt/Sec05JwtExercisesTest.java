package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Sec05Jwt. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Sec05JwtExercisesTest {

    static String base64Url(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Ćwiczenie 1: odczyt ładunku tokenu bez sprawdzania podpisu")
    void exercise1() {
        String token = base64Url("{\"alg\":\"none\"}") + "." + base64Url("{\"sub\":\"anna\",\"scope\":\"products:read\"}") + ".x";
        assertThat(Sec05Jwt.exercise1(token)).isEqualTo("{\"sub\":\"anna\",\"scope\":\"products:read\"}");
        assertThat(Sec05Jwt.exercise1("eyJhbGciOiJub25lIn0.eyJzdWIiOiJ6b2ZpYSJ9.x")).isEqualTo("{\"sub\":\"zofia\"}");
        assertThatThrownBy(() -> Sec05Jwt.exercise1("tylko.dwie")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: przeterminowany z tolerancją zegara (jak JwtTimestampValidator)")
    void exercise2() {
        Instant exp = Instant.parse("2026-10-19T12:00:00Z");
        Duration skew = Duration.ofSeconds(60);
        assertThat(Sec05Jwt.exercise2(exp, Instant.parse("2026-10-19T11:59:00Z"), skew)).isFalse();
        assertThat(Sec05Jwt.exercise2(exp, Instant.parse("2026-10-19T12:00:59Z"), skew)).isFalse();
        assertThat(Sec05Jwt.exercise2(exp, Instant.parse("2026-10-19T12:01:00Z"), skew)).as("dokładnie na granicy").isFalse();
        assertThat(Sec05Jwt.exercise2(exp, Instant.parse("2026-10-19T12:01:01Z"), skew)).isTrue();
        assertThat(Sec05Jwt.exercise2(exp, Instant.parse("2026-10-19T12:00:01Z"), Duration.ZERO)).isTrue();
    }

    @Test
    @DisplayName("Ćwiczenie 3: uprawnienia z claimu „permissions” bez prefiksu (+ FACTOR_BEARER od Security 7)")
    void exercise3() {
        Jwt jwt = Jwt.withTokenValue("token-testowy")
                .header("alg", "RS256")
                .subject("tomasz")
                .claim("permissions", List.of("report:read", "orders:write"))
                .claim("scope", "products:read")
                .build();

        var authentication = Sec05Jwt.exercise3().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("report:read", "orders:write", "FACTOR_BEARER");
    }

    @Test
    @DisplayName("Ćwiczenie 4: wystawienie tokenu z iss, sub, iat, exp (+15 min) i scope")
    void exercise4() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();   // klucze TESTOWE, tylko w pamięci
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withKeyPair((RSAPublicKey) keys.getPublic(),
                (RSAPrivateKey) keys.getPrivate()).build();
        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Jwt jwt = decoder.decode(Sec05Jwt.exercise4(encoder, "piotr", now));

        assertThat(jwt.getClaimAsString("iss")).isEqualTo(Sec05JwtConfig.ISSUER);
        assertThat(jwt.getSubject()).isEqualTo("piotr");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("products:read");
    }
}
