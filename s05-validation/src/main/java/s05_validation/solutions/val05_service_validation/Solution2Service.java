package s05_validation.solutions.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import s05_validation.val05_service_validation.ProductCommand;

/** Rozwiązanie ćwiczenia 2: parametr @Valid @NotNull, wynik z @Pattern. */
@Service
@Validated
public class Solution2Service {

    public @Pattern(regexp = "[A-Z]{3}-\\d{4}") String codeFor(@Valid @NotNull ProductCommand command) {
        String name = command.name().toUpperCase();
        String prefix = name.substring(0, Math.min(3, name.length()));
        return prefix + "-" + String.format("%04d", command.priceGrosze() % 10_000);
    }
}
