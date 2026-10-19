package s12_security.sec03_users_passwords;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MeController = „kim jestem?” — trzy sposoby odczytania zalogowanego użytkownika w kontrolerze (sekcja 5 lekcji)
 * i jeden ZŁY przykład, przez który wycieka skrót hasła (sekcja 6).
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    /** DTO (obiekt do przesyłania danych): tylko to, co klient MA zobaczyć — bez hasła. */
    public record Me(String username, List<String> authorities) {
    }

    private final CurrentUserService currentUser;
    private final UserDetailsService users;

    public MeController(CurrentUserService currentUser, UserDetailsService users) {
        this.currentUser = currentUser;
        this.users = users;
    }

    /**
     * Sposób 1: {@code @AuthenticationPrincipal} (podmiot uwierzytelnienia) — Spring MVC wstrzykuje principal,
     * czyli obiekt UserDetails zwrócony przez ShopUserDetailsService.
     */
    @GetMapping
    public Me me(@AuthenticationPrincipal UserDetails user) {
        List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
        return new Me(user.getUsername(), authorities);
    }

    /**
     * Sposób 2: parametr {@code Authentication} (uwierzytelnienie) — cały „bilet” zalogowanego: login, uprawnienia,
     * principal i credentials (dane uwierzytelniające). Po zalogowaniu Spring USUWA hasło z biletu (credentials = null).
     */
    @GetMapping("/authentication")
    public Map<String, Object> authentication(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        // uprawnienia BILETU — w Security 7 zawierają też FACTOR_PASSWORD („zalogowany hasłem”), a uprawnienia
        // z UserDetails (sposób 1) — nie
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return Map.of(
                "name", authentication.getName(),
                "type", authentication.getClass().getSimpleName(),
                "authorities", authorities,
                "credentialsIsNull", authentication.getCredentials() == null,
                "principalPasswordIsNull", principal.getPassword() == null);
    }

    /** Sposób 3: SecurityContextHolder w serwisie — także w innych wątkach. */
    @GetMapping("/threads")
    public Map<String, String> threads() throws InterruptedException {
        return currentUser.whoIsLoggedInWhere();
    }

    /**
     * ZŁY PRZYKŁAD (nie rób tak!): zwraca cały UserDetails świeżo wczytany z „bazy”. Jackson zamienia na JSON WSZYSTKIE
     * gettery — także getPassword(), czyli skrót hasła. Poprawnie: DTO jak rekord Me.
     */
    @GetMapping("/leaky")
    public UserDetails leaky(Authentication authentication) {
        return users.loadUserByUsername(authentication.getName());
    }
}
