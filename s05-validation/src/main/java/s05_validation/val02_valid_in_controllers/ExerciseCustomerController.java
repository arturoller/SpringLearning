package s05_validation.val02_valid_in_controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ExerciseCustomerController = SZKIELET do ćwiczeń 1 i 2 lekcji Val02ValidInControllers (kontroler jest „zepsuty”
 * celowo — Twoje zadanie to go naprawić adnotacjami). Rozwiązanie wzorcowe: pakiet solutions.val02_valid_in_controllers.
 */
@RestController
@RequestMapping("/api/exercise/customers")
public class ExerciseCustomerController {

    // TODO ćwiczenie 1: zły formularz (pusta nazwa, zły e-mail) ma dawać 400 — czego brakuje przy parametrze?
    @PostMapping
    public ResponseEntity<Map<String, String>> register(@RequestBody CustomerRequest request) {
        return ResponseEntity.status(201).body(Map.of("email", request.email()));
    }

    // TODO ćwiczenie 2: id mniejsze od 1 ma dawać 400 (bez @Validated na klasie!)
    @GetMapping("/{id}")
    public Map<String, Long> one(@PathVariable long id) {
        return Map.of("id", id);
    }
}
