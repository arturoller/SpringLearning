package s21_capstone.cap03_library_with_auth.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Logowanie: login i hasło w JSON-ie → token JWT. Hasło sprawdza AuthenticationManager (DaoAuthenticationProvider:
 * UserDetailsService + BCrypt). Zły login albo hasło → BadCredentialsException → porada → 401 z TYM SAMYM opisem
 * w obu przypadkach (atakujący nie dowie się, które konta istnieją).
 * <pre>
 *   POST /api/auth/login  PUBLICZNY  200 {"access_token":"eyJ...","token_type":"Bearer","expires_in":3600} / 400 / 401
 *   GET  /api/me          zalogowany 200 {"username":"jan","authorities":[...]}
 * </pre>
 */
@RestController
public class AuthController {

    /** Żądanie logowania. */
    public record LoginRequest(
            @NotBlank(message = "login jest wymagany") String username,
            @NotBlank(message = "hasło jest wymagane") String password) {
    }

    /**
     * Odpowiedź w kształcie OAuth 2 (RFC 6749): pola snake_case. {@code @JsonProperty} w Jacksonie 3 nadal pochodzi
     * z pakietu com.fasterxml.jackson.annotation (adnotacje zostały w starym pakiecie, reszta przeszła do tools.jackson).
     */
    public record TokenResponse(@JsonProperty("access_token") String accessToken,
                                @JsonProperty("token_type") String tokenType,
                                @JsonProperty("expires_in") long expiresIn) {
    }

    public record MeResponse(String username, List<String> authorities) {
    }

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/api/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return new TokenResponse(tokenService.issue(authentication), "Bearer", TokenService.VALIDITY.toSeconds());
    }

    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName(),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }
}
