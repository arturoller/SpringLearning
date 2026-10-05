package s00_start.solutions.start06_exercises;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s00_start.start06_exercises.DiscountController.DiscountResponse;

import java.util.Map;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Start06Exercises — kontroler rabatów. */
@RestController
public class SolutionDiscountController {

    @GetMapping("/api/discount")
    public DiscountResponse discount(@RequestParam int price, @RequestParam int percent) {
        return new DiscountResponse(price, percent, Start06ExercisesSolutions.solution1(price, percent));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
