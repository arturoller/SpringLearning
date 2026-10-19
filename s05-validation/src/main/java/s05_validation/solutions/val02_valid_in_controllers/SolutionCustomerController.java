package s05_validation.solutions.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s05_validation.val02_valid_in_controllers.CustomerRequest;

import java.util.Map;

/** Rozwiązanie wzorcowe ćwiczeń 1 i 2 lekcji Val02ValidInControllers (ExerciseCustomerController po naprawie). */
@RestController
@RequestMapping("/api/exercise/customers")
public class SolutionCustomerController {

    @PostMapping
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody CustomerRequest request) {   // @Valid uruchamia walidację
        return ResponseEntity.status(201).body(Map.of("email", request.email()));
    }

    @GetMapping("/{id}")
    public Map<String, Long> one(@PathVariable @Min(1) long id) {   // ograniczenie na zmiennej ze ścieżki, bez @Validated
        return Map.of("id", id);
    }
}
