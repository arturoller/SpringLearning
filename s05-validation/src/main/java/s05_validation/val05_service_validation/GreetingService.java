package s05_validation.val05_service_validation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * GreetingService = SZKIELET do ćwiczenia 2 lekcji Val05ServiceValidation. Klasa ma już {@code @Validated}, ale
 * ograniczenie stoi w złym miejscu: metoda {@code clean} jest wywoływana z {@code greet} przez {@code this}, więc proxy
 * jej nie widzi. TODO: napraw tak, aby {@code greet(" ")} wołane z zewnątrz rzucało ConstraintViolationException.
 * Rozwiązanie wzorcowe: solutions.val05_service_validation.
 */
@Service
@Validated
public class GreetingService {

    public String greet(String name) {   // TODO ćwiczenie 2: tu brakuje ograniczenia
        return "Witaj, " + clean(name) + "!";
    }

    String clean(@NotBlank String name) {
        return name.strip();
    }
}
