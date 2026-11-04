package s12_security.sec04_stateless_api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Kontroler do ĆWICZENIA 4 lekcji Sec04StatelessApi — GOTOWY, nic tu nie zmieniasz. Reguły dla tych adresów piszesz
 * w {@link ExerciseSecurityConfig#filterChain}. W aplikacji lekcji te adresy zamyka {@code anyRequest().denyAll()}
 * łańcucha przeglądarki.
 */
@RestController
@RequestMapping("/sec04/cw")
public class ExerciseController {

    @GetMapping("/catalog")
    public String catalog() {
        return "Kawa, herbata, czekolada";
    }

    @GetMapping("/orders")
    public String orders(Principal principal) {
        return "Zamówienia: " + principal.getName();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(Principal principal) {
        return "Przyjęto zamówienie od " + principal.getName();
    }
}
