package s12_security.sec04_stateless_api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * WebController = „strony” dla przeglądarki (tu zwykły tekst, bez HTML-a). Obsługuje je łańcuch webChain:
 * formularz logowania, sesja w ciasteczku JSESSIONID i ochrona CSRF.
 */
@RestController
@RequestMapping("/web")
public class WebController {

    @GetMapping("/profile")
    public String profile(Principal principal) {
        return "Profil: " + principal.getName();
    }

    /**
     * Token CSRF dla tej sesji. {@code CsrfToken} Spring MVC wstrzykuje z zapytania (filtr CsrfFilter go tam włożył).
     * Normalnie token trafia do ukrytego pola formularza HTML albo do ciasteczka XSRF-TOKEN (aplikacje SPA).
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "parameterName", token.getParameterName(),
                "token", token.getToken());
    }

    /** Zapis na newsletter — zmiana stanu metodą POST, więc chroniona tokenem CSRF. */
    @PostMapping("/newsletter")
    public String subscribe(Principal principal) {
        return "Zapisano na newsletter: " + principal.getName();
    }
}
