package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ValidatedClassController = kontroler ze STARYM sposobem: {@code @Validated} na klasie (sekcja 3, pułapka).
 * Walidację wykonuje wtedy proxy AOP i rzuca ConstraintViolationException, a nie HandlerMethodValidationException.
 */
@RestController
@Validated   // validated = zwalidowany; na klasie włącza walidację przez proxy (stary sposób, sprzed Frameworka 6.1)
@RequestMapping("/validated")
public class ValidatedClassController {

    @GetMapping("/items")
    public Map<String, Object> items(@RequestParam @Positive int limit) {
        return Map.of("limit", limit);
    }
}
