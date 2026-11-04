package s12_security.sec05_jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Wydawanie tokenów JWT (sekcja 2 lekcji Sec05Jwt). Czas bierzemy z wstrzykiwanego {@link Clock} — dzięki temu test może
 * "cofnąć zegar" i dostać token, który wygasł, bez czekania ({@code Thread.sleep}).
 */
@Service
public class TokenService {

    /** Wystawca (issuer) tokenów — kto podpisał token. */
    public static final String ISSUER = "sklep-sec05";

    /** Krótki czas życia: skradziony token przyda się złodziejowi najwyżej kwadrans (sekcja 5). */
    public static final Duration LIFETIME = Duration.ofMinutes(15);

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    /** Token dla użytkownika, który właśnie zalogował się hasłem (Basic). */
    public TokenResponse issue(Authentication authentication) {
        Instant now = clock.instant();
        // Do tokenu trafiają tylko ROLE (bez prefiksu). Inne uprawnienia — np. FACTOR_PASSWORD, które Security 7 dodaje
        // po logowaniu hasłem ("czym się logowałeś") — nie opisują, co użytkownikowi wolno.
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()   // claims = "twierdzenia", czyli dane w tokenie
                .issuer(ISSUER)                         // iss — kto wystawił
                .subject(authentication.getName())      // sub — kogo dotyczy
                .issuedAt(now)                          // iat — kiedy wystawiony
                .expiresAt(now.plus(LIFETIME))          // exp — do kiedy ważny
                .claim("roles", roles)                  // własne twierdzenie: role użytkownika
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();   // podpis HMAC SHA-256 (sekcja 1)
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", LIFETIME.toSeconds());
    }
}
