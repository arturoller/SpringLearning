package s12_security.solutions.sec05_jwt;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import s12_security.sec05_jwt.Sec05JwtConfig;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/** Rozwiązania wzorcowe ćwiczeń lekcji Sec05Jwt — zajrzyj dopiero po własnej próbie! */
public final class Sec05JwtSolutions {

    private Sec05JwtSolutions() {
    }

    static String solution1(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT ma trzy części rozdzielone kropkami, a jest: " + parts.length);
        }
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    static boolean solution2(Instant expiresAt, Instant now, Duration clockSkew) {
        return now.minus(clockSkew).isAfter(expiresAt);
    }

    static JwtAuthenticationConverter solution3() {
        JwtGrantedAuthoritiesConverter permissions = new JwtGrantedAuthoritiesConverter();
        permissions.setAuthoritiesClaimName("permissions");
        permissions.setAuthorityPrefix("");   // bez prefiksu: „report:read” zostaje „report:read”
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(permissions);
        return converter;
    }

    static String solution4(JwtEncoder encoder, String username, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(Sec05JwtConfig.ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .claim("scope", "products:read")
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
