package s12_security.sec05_jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * API sklepu lekcji Sec05Jwt chronione tokenem ({@code Authorization: Bearer …}). Kontroler widzi zalogowanego użytkownika
 * jak zawsze ({@code Principal}, {@code Authentication}), tylko że dane pochodzą z TOKENU, a nie z bazy użytkowników.
 */
@RestController
@RequestMapping("/sec05")
public class ShopController {

    /** Kim jestem według tokenu: nazwa, uprawnienia wyliczone z twierdzeń i czas wygaśnięcia. */
    public record Me(String name, List<String> authorities, List<String> rolesClaim, Instant expiresAt) {
    }

    @GetMapping("/products")
    public List<String> products() {
        return List.of("Kawa ziarnista", "Herbata zielona");
    }

    @GetMapping("/orders")
    public String orders(JwtAuthenticationToken authentication) {   // JwtAuthenticationToken = uwierzytelnienie z JWT
        return "Zamówienia użytkownika " + authentication.getName();
    }

    @GetMapping("/admin/stats")
    public String adminStats() {
        return "Liczba zamówień: 3";
    }

    @GetMapping("/me")
    public Me me(JwtAuthenticationToken authentication, @AuthenticationPrincipal Jwt jwt) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return new Me(authentication.getName(), authorities, jwt.getClaimAsStringList("roles"), jwt.getExpiresAt());
    }
}
