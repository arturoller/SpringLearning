package s04_web_rest.rest06_cors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ćwiczenie 1 lekcji Rest06Cors (pozostałe — w klasie Rest06Exercises). Testy: Rest06CorsExercisesTest
 * (uruchamiasz sam: ▶ obok klasy testu).
 */
@RestController
@RequestMapping("/api/cw06")
public class Rest06ExerciseController {

    /**
     * ĆWICZENIE 1 (łatwe): GET /api/cw06/promotions zwraca listę ["Kawa -10%", "Czekolada 2+1"]. Nowy frontend
     * promocji działa pod http://localhost:5173 (serwer deweloperski Vite) i TYLKO on może czytać te dane
     * z przeglądarki. Uzupełnij ciało i dodaj adnotację {@code @CrossOrigin} z właściwym pochodzeniem.
     * Inne pochodzenie ma dostać 403.
     */
    @GetMapping("/promotions")
    public List<String> promotions() {
        // TODO: twoje rozwiązanie (dodaj też @CrossOrigin nad metodą)
        throw new UnsupportedOperationException("TODO");
    }
}
