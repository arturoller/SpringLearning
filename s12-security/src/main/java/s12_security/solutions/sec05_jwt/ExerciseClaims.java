package s12_security.solutions.sec05_jwt;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Sec05Jwt. */
public final class ExerciseClaims {

    private ExerciseClaims() {
    }

    public static JwtClaimsSet claimsFor(String username, List<String> roles, Instant now) {
        return JwtClaimsSet.builder()
                .issuer("sklep-cw")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(10)))
                .claim("roles", roles)
                .build();
    }
}
