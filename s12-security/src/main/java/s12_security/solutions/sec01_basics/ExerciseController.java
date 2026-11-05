package s12_security.solutions.sec01_basics;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rozwiązanie wzorcowe kontrolera ćwiczeń lekcji Sec01Basics (ćwiczenie 1). */
@RestController
@RequestMapping("/sec01/cw")
public class ExerciseController {

    @GetMapping("/greeting")
    public String greeting(Authentication authentication) {
        return "Dzień dobry, " + authentication.getName();
    }

    @GetMapping("/public/info")
    public String publicInfo() {
        return "Sklep czynny 8–20";
    }

    @GetMapping("/admin/stats")
    public String adminStats() {
        return "Zamówień dzisiaj: 42";
    }
}
