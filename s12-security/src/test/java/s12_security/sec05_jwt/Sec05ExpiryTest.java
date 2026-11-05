package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 3 lekcji Sec05Jwt: wygaśnięcie tokenu sprawdzane z PRZESUNIĘTYM zegarem ({@code Clock.offset}) — bez czekania.
 * Token wystawiony o 10:00 żyje 15 minut; dekoder "myśli", że jest 10:14, 10:15:30 albo 10:17.
 */
class Sec05ExpiryTest {

    private static final Clock ISSUED = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);

    private final Sec05SecurityConfig config = new Sec05SecurityConfig();

    @Test
    @DisplayName("Sekcja 3: przed exp token jest ważny, 30 s po exp jeszcze tak (60 s tolerancji zegarów), 2 minuty po — Jwt expired at")
    void expiryWithClockSkew() throws Exception {
        SecretKey key = config.jwtSecretKey();
        String token = new TokenService(NimbusJwtEncoder.withSecretKey(key).build(), ISSUED)
                .issue(new UsernamePasswordAuthenticationToken("anna", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")))
                .accessToken();

        assertThat(config.jwtDecoder(key, Clock.offset(ISSUED, Duration.ofMinutes(14))).decode(token).getSubject())
                .isEqualTo("anna");
        assertThat(config.jwtDecoder(key, Clock.offset(ISSUED, Duration.ofSeconds(15 * 60 + 30))).decode(token))
                .as("domyślna tolerancja (clock skew) to 60 sekund").isNotNull();
        assertThatThrownBy(() -> config.jwtDecoder(key, Clock.offset(ISSUED, Duration.ofMinutes(17))).decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("Jwt expired at 2026-11-04T10:15:00Z");
    }
}
