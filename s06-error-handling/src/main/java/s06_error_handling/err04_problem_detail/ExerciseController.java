package s06_error_handling.err04_problem_detail;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * SZKIELET ĆWICZEŃ lekcji Err04ProblemDetail — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Err04ProblemDetailExercisesTest). Mapowania są gotowe. Dopóki metody rzucają UnsupportedOperationException("TODO"),
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}). Handlery piszesz w klasie {@link ExerciseAdvice}.
 */
@RestController
@RequestMapping("/err04/cw")
public class ExerciseController {

    public static class CustomerNotFoundException extends RuntimeException {

        public CustomerNotFoundException(long id) {
            super("Nie ma klienta " + id);
        }
    }

    /** Ćwiczenie 1: dla id 1 zwróć {@code "Anna"}, dla innych rzuć {@link CustomerNotFoundException}. */
    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: rzuć {@code ErrorResponseException} (status 409) z type, title, detail i właściwością code — bez handlera. */
    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: zwróć {@code "Szukam: " + q}. Brak parametru {@code q} obsłuż w {@link ExerciseAdvice}. */
    @GetMapping("/search")
    public String search(@RequestParam String q) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: rzuć {@code new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'")}. */
    @GetMapping("/crash")
    public String crash() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5: zwróć {@code "Utworzono: " + request.name()}. Błędy walidacji obsłuż w {@link ExerciseAdvice}. */
    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public String create(@Valid @RequestBody NewCustomer request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
