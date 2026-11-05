package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kontroler "po staremu": {@code @Validated} na KLASIE (tak robiły to tutoriale sprzed Springa 6.1). Walidacja parametrów
 * idzie wtedy inną drogą — przez proxy (Val05ServiceValidation) — i rzuca {@code jakarta.validation.ConstraintViolationException},
 * a nie {@code HandlerMethodValidationException}.
 */
@RestController
@RequestMapping("/val02/legacy/products")
@Validated
public class LegacyValidatedController {

    @GetMapping("/{id}")
    public String find(@PathVariable @Min(1) long id) {
        return "produkt " + id;
    }
}
