package s12_security.sec06_security_testing;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wydawanie tokenów lekcji Sec06SecurityTesting — w skrócie to samo, co TokenService z Sec05Jwt (tam opis krok po kroku).
 * Odpowiedź: {@code {"access_token": "…", "token_type": "Bearer", "expires_in": 900}}.
 */
@RestController
public class TokenController {

    private static final Duration LIFETIME = Duration.ofMinutes(15);

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenController(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    @PostMapping("/sec06/token")
    public Map<String, Object> token(Authentication authentication) {
        Instant now = clock.instant();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sklep-sec06").subject(authentication.getName())
                .issuedAt(now).expiresAt(now.plus(LIFETIME))
                .claim("roles", roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return Map.of("access_token", token, "token_type", "Bearer", "expires_in", LIFETIME.toSeconds());
    }
}
