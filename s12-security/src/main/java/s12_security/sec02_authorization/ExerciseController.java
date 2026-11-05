package s12_security.sec02_authorization;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kontroler do ĆWICZENIA 4 lekcji Sec02Authorization — GOTOWY, nic tu nie zmieniasz. Reguły dla tych adresów piszesz
 * w {@link ExerciseSecurityConfig#filterChain}. W aplikacji lekcji te adresy zamyka {@code anyRequest().denyAll()}.
 */
@RestController
@RequestMapping("/sec02/cw")
public class ExerciseController {

    @GetMapping("/catalog/coffee")
    public String coffee() {
        return "Kawa ziarnista";
    }

    @GetMapping("/catalog/hidden/prototype")
    public String hiddenPrototype() {
        return "Nowa kawa (jeszcze tajna)";
    }

    @PostMapping("/catalog")
    @ResponseStatus(HttpStatus.CREATED)
    public String addToCatalog() {
        return "Dodano";
    }

    @GetMapping("/secret")
    public String secret() {
        return "Nikt nie powinien tego zobaczyć";
    }
}
