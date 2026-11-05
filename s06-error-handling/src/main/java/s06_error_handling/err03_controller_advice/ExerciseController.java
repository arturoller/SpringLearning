package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SZKIELET ĆWICZEŃ lekcji Err03ControllerAdvice — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Err03ControllerAdviceExercisesTest). Mapowania są gotowe. Dopóki metody rzucają UnsupportedOperationException("TODO"),
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}). Handlery piszesz w klasach
 * {@link ExerciseAdvice}, {@link ExerciseUrgentAdvice}, {@link ExerciseAdvisoryAdvice} i {@link ExerciseVipController}.
 */
@RestController
@RequestMapping("/err03/cw")
public class ExerciseController {

    public static class CustomerNotFoundException extends RuntimeException {

        public CustomerNotFoundException(long id) {
            super("Nie ma klienta " + id);
        }
    }

    public static class OrderLockedException extends RuntimeException {

        public OrderLockedException(long id) {
            super("Zamówienie " + id + " jest zablokowane");
        }
    }

    public static class ExercisePriorityException extends RuntimeException {
    }

    /** Ćwiczenie 1: dla id 1 zwróć {@code "Anna"}, dla innych rzuć {@link CustomerNotFoundException}. */
    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: zawsze rzuć {@link OrderLockedException} dla podanego id. */
    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: rzuć {@code new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'")}. */
    @GetMapping("/crash")
    public String crash() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5: rzuć {@link ExercisePriorityException}. */
    @GetMapping("/priority")
    public String priority() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
