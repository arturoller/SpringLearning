package s12_security.sec05_jwt;

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
 * TokenService = wystawianie tokenów JWT po zalogowaniu hasłem (sekcja 3 lekcji Sec05Jwt).
 *
 * <p>W prawdziwym systemie tę pracę robi serwer tożsamości (Keycloak, Spring Authorization Server, Auth0...),
 * a nasza aplikacja tylko SPRAWDZA tokeny. Tu wystawiamy je sami, żeby zobaczyć cały obieg.</p>
 */
@Service
public class TokenService {

    /** Jak długo token jest ważny. Krótko — bo raz wydanego tokenu nie da się łatwo „odwołać”. */
    public static final Duration VALIDITY = Duration.ofHours(1);

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    /** Token dla zalogowanego użytkownika: zakres (scope) i role wyliczone z jego ról w sklepie. */
    public String issue(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
        String scope = roles.contains("ADMIN") ? "products:read products:write" : "products:read";
        return issue(authentication.getName(), scope, roles, clock.instant(), VALIDITY);
    }

    /** Wersja „techniczna”: pełna kontrola nad treścią i czasem (testy używają jej do tokenu przeterminowanego). */
    public String issue(String username, String scope, List<String> roles, Instant issuedAt, Duration validity) {
        JwtClaimsSet claims = JwtClaimsSet.builder()        // claims = „twierdzenia” zapisane w tokenie
                .issuer(Sec05JwtConfig.ISSUER)               // iss — kto wystawił
                .subject(username)                           // sub — kogo dotyczy
                .issuedAt(issuedAt)                          // iat — kiedy wystawiony
                .expiresAt(issuedAt.plus(validity))          // exp — do kiedy ważny
                .claim("scope", scope)                       // zakres uprawnień (rozdzielany spacjami)
                .claim("roles", roles)                       // nasz własny claim z rolami
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
