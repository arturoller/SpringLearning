package s12_security.sec01_basics;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SZKIELET ĆWICZEŃ lekcji Sec01Basics — kontroler z gotowymi mapowaniami (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Sec01BasicsExercisesTest i Sec01ExerciseSecurityTest, oba z {@code @Tag("cwiczenie")}).
 * Metody {@code publicInfo} i {@code adminStats} są gotowe — przydadzą się w ćwiczeniu 5 (Twoje reguły adresów).
 */
@RestController
@RequestMapping("/sec01/cw")
public class ExerciseController {

    /** Ćwiczenie 1: zwróć {@code "Dzień dobry, " + nazwa zalogowanego użytkownika}. */
    @GetMapping("/greeting")
    public String greeting(Authentication authentication) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
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
