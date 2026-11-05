package s12_security.sec01_basics;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Kontroler lekcji Sec01Basics. Sam kontroler NIE sprawdza haseł ani ról — robią to filtry Spring Security, zanim żądanie
 * w ogóle dotrze do tej klasy. Kontroler najwyżej pyta, KTO jest zalogowany (sekcja 5).
 */
@RestController
@RequestMapping("/sec01")
public class Sec01Controller {

    /** Odpowiedź {@code /sec01/me}: kim jest zalogowany użytkownik, widziany na trzy sposoby. */
    public record Me(String name, String principalName, String userDetailsName, List<String> authorities,
                     boolean sameAsSecurityContext) {
    }

    @GetMapping("/public/hello")
    public String publicHello() {
        return "Witaj, gościu! Ten adres jest publiczny.";
    }

    @GetMapping("/public/crash")
    public String publicCrash() {
        // Publiczny adres, który kończy się błędem 500 — do PUŁAPKI z /error (sekcja 5).
        throw new IllegalStateException("Awaria magazynu (komunikat techniczny, nie dla klienta)");
    }

    @GetMapping("/hello")
    public String hello(Principal principal) {
        return "Cześć, " + principal.getName() + "!";
    }

    @GetMapping("/me")
    public Me me(Authentication authentication,                  // authentication = uwierzytelnienie: kto + jakie uprawnienia
                 Principal principal,                            // principal = podmiot (interfejs z Javy: tylko getName())
                 @AuthenticationPrincipal UserDetails user) {    // sam obiekt użytkownika z UserDetailsService
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        boolean same = SecurityContextHolder.getContext().getAuthentication() == authentication;
        return new Me(authentication.getName(), principal.getName(), user.getUsername(), authorities, same);
    }

    @GetMapping("/admin/panel")
    public String adminPanel() {
        return "Panel administratora";
    }
}
