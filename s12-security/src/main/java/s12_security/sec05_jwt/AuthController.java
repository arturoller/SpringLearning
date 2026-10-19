package s12_security.sec05_jwt;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AuthController = „okienko wymiany”: login i hasło (HTTP Basic) → token JWT.
 * Odpowiedź ma pola jak w OAuth 2 (RFC 6749): access_token, token_type, expires_in.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenService tokens;

    public AuthController(TokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/token")
    public Map<String, Object> token(Authentication authentication) {
        return Map.of(
                "access_token", tokens.issue(authentication),
                "token_type", "Bearer",
                "expires_in", TokenService.VALIDITY.toSeconds());
    }
}
