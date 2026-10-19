package s05_validation.val02_valid_in_controllers;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ValidatedController = kontroler Z {@code @Validated} NA KLASIE — stary sposób sprzed Springa 6.1. Spring MVC widzi
 * tę adnotację i oddaje walidację parametrów proxy (AOP), więc błąd to {@code ConstraintViolationException}, a nie
 * {@code HandlerMethodValidationException}.
 */
@RestController
@RequestMapping("/api/validated")
@Validated
public class ValidatedController {

    @GetMapping
    public Map<String, Object> list(@RequestParam @Min(1) int limit) {
        return Map.of("limit", limit);
    }
}
