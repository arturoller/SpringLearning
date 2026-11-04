package s12_security.sec05_jwt;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punkt końcowy (endpoint) wydawania tokenów lekcji Sec05Jwt. Logowanie hasłem (HTTP Basic) załatwia łańcuch
 * {@code tokenChain} — tu trafia już zalogowany użytkownik i dostaje token.
 */
@RestController
public class TokenController {

    private final TokenService tokens;

    public TokenController(TokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/sec05/token")
    public TokenResponse token(Authentication authentication) {
        return tokens.issue(authentication);
    }
}
