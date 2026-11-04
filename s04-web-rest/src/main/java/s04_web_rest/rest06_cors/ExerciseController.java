package s04_web_rest.rest06_cors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kontroler do ĆWICZENIA 5 lekcji Rest06Cors — jest GOTOWY (nic tu nie zmieniasz). Ćwiczenie polega na zbudowaniu
 * źródła konfiguracji CORS w {@link ExerciseCors#source()}, które test podłącza przed ten kontroler jako filtr.
 */
@RestController
@RequestMapping("/rest06/cw/orders")
public class ExerciseController {

    @GetMapping
    public String orders() {
        return "zamówienia";
    }

    @PostMapping
    public String create() {
        return "utworzono";
    }
}
