package s21_capstone.cap03_library_with_auth.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Wystawianie tokenów JWT po udanym logowaniu (jak TokenService z s12_security/Sec05Jwt). W tokenie tylko to, czego
 * API potrzebuje do decyzji: kto (sub), jakie role (roles), kto wydał (iss), do kiedy ważny (exp). „Teraz” — z Clock.
 */
@Service
public class TokenService {

    /** Wystawca (claim iss) — dekoder odrzuci token z innym. Domena .example jest zarezerwowana na przykłady. */
    public static final String ISSUER = "https://biblioteka.example";
    public static final Duration VALIDITY = Duration.ofHours(1);

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    /** Token dla uwierzytelnionego użytkownika: role bez prefiksu ROLE_ (np. ["BIBLIOTEKARZ"]). */
    public String issue(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        return issue(authentication.getName(), roles, clock.instant(), VALIDITY);
    }

    /** Wersja „techniczna” z pełną kontrolą czasu — testy wystawiają nią token przeterminowany. */
    public String issue(String username, List<String> roles, Instant issuedAt, Duration validity) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(validity))
                .claim("roles", roles)
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
