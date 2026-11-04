package s12_security.sec04_stateless_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * "Strona dla przeglądarki" lekcji Sec04StatelessApi ({@code /sec04/web/**}): logowanie raz, potem sesja (ciasteczko
 * {@code JSESSIONID}) i dlatego OCHRONA CSRF (sekcja 2). W prawdziwej aplikacji byłyby tu szablony HTML z formularzami —
 * dla testów wystarczą odpowiedzi tekstowe.
 */
@RestController
@RequestMapping("/sec04/web")
public class BrowserPageController {

    @GetMapping("/profile")
    public String profile(Principal principal) {
        return "Profil: " + principal.getName();
    }

    /** Zmiana adresu e-mail — klasyczny cel ataku CSRF (napastnik zmienia e-mail, potem "resetuje" hasło). */
    @PostMapping("/email")
    public String changeEmail(@RequestParam String email, Principal principal) {
        return "Nowy e-mail użytkownika " + principal.getName() + ": " + email;
    }
}
