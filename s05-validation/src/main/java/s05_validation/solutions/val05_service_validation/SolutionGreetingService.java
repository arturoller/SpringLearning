package s05_validation.solutions.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Val05ServiceValidation. */
@Service
@Validated
public class SolutionGreetingService {

    /** Ograniczenie przeniesione na metodę wołaną Z ZEWNĄTRZ (przez proxy) — to ona jest granicą serwisu. */
    public String greet(@NotBlank String name) {
        return "Witaj, " + clean(name) + "!";
    }

    String clean(String name) {
        return name.strip();
    }
}
