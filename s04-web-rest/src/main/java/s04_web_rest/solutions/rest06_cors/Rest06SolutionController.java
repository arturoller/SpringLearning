package s04_web_rest.solutions.rest06_cors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Rest06Cors — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/api/cw06")
public class Rest06SolutionController {

    /** Rozwiązanie 1: @CrossOrigin z jednym, konkretnym pochodzeniem; inne dostaną 403. */
    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/promotions")
    public List<String> promotions() {
        return List.of("Kawa -10%", "Czekolada 2+1");
    }
}
