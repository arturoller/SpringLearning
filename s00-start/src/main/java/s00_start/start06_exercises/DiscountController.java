package s00_start.start06_exercises;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DiscountController = kontroler rabatów — SZKIELET ćwiczenia 3 z lekcji Start06Exercises.
 * Adres i parametry są gotowe; uzupełnij ciało metody. Test: Start06ExercisesExercisesTest (@WebMvcTest).
 */
@RestController
public class DiscountController {

    /** DiscountResponse = odpowiedź z rabatem: {"price":10000,"percent":15,"discounted":8500}. */
    public record DiscountResponse(int price, int percent, int discounted) {
    }

    /**
     * Ćwiczenie 3 (opis w Start06Exercises): {@code GET /api/discount?price=10000&percent=15} ma zwrócić
     * DiscountResponse(10000, 15, 8500). Użyj swojego Start06Exercises.exercise1 (cena po rabacie).
     * Dla procentu spoza 0..100 (Start06Exercises.exercise2 rzuca wyjątek) odpowiedź ma mieć status 400 —
     * dopisz metodę z {@code @ExceptionHandler(IllegalArgumentException.class)} jak w PriceController (Start04TestingIntro).
     */
    @GetMapping("/api/discount")
    public DiscountResponse discount(@RequestParam int price, @RequestParam int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
