package s05_validation.solutions.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń 1-3 lekcji Val02ValidInControllers (te same adresy co w ExerciseController). */
@RestController
@RequestMapping("/exercise")
public class SolutionController {

    public record OrderRequest(@NotBlank String customer, @Positive int quantity) {
    }

    public record CustomerRequest(@NotBlank String name, @Email String email) {
    }

    /** Rozwiązanie 1: @Valid przy @RequestBody. */
    @PostMapping("/orders")
    public Map<String, Object> createOrder(@Valid @RequestBody OrderRequest request) {
        return Map.of("customer", request.customer(), "quantity", request.quantity());
    }

    /** Rozwiązanie 2: ograniczenia przy parametrach, bez @Validated na klasie. */
    @GetMapping("/orders/{id}")
    public Map<String, Object> findOrder(@PathVariable @Min(1) long id,
                                         @RequestParam @Min(1) @Max(99) int quantity) {
        return Map.of("id", id, "quantity", quantity);
    }

    @PostMapping("/customers")
    public Map<String, Object> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return Map.of("name", request.name());
    }

    /** Rozwiązanie 3: pole → komunikat; przy duplikatach zostaje pierwszy komunikat. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Map<String, String>>> onInvalid(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(fe -> fe.getField(), fe -> String.valueOf(fe.getDefaultMessage()),
                        (first, second) -> first, TreeMap::new));
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
}
