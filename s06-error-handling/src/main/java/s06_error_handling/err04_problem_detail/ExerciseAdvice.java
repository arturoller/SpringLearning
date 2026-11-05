package s06_error_handling.err04_problem_detail;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Ćwiczenia 1, 3, 4 i 5: tutaj dopisujesz handlery. Advice jest zawężony do {@link ExerciseController} (assignableTypes) i ma
 * najwyższe pierwszeństwo, więc nie koliduje z {@code Err04Advice} z lekcji. Dziedziczy po {@code ResponseEntityExceptionHandler},
 * więc wyjątki Springa już teraz dają ProblemDetail — Ty dopisujesz resztę.
 * <p>
 * Do napisania:
 * <ul>
 *   <li>ćwiczenie 1: {@code CustomerNotFoundException} → ProblemDetail 404, title "Klient nie istnieje", detail z wyjątku,
 *       właściwość code = "CUSTOMER_NOT_FOUND";</li>
 *   <li>ćwiczenie 3: nadpisz {@code handleMissingServletRequestParameter} → 400, właściwość code = "MISSING_PARAMETER"
 *       i parameter = nazwa brakującego parametru;</li>
 *   <li>ćwiczenie 4: {@code Exception} → 500, detail "Wystąpił nieoczekiwany błąd", code = "UNEXPECTED", szczegóły do logu;</li>
 *   <li>ćwiczenie 5: nadpisz {@code handleMethodArgumentNotValid} → 400, title "Błąd walidacji", code = "VALIDATION_FAILED"
 *       i właściwość fields: mapa pole → komunikat (posortowana).</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = ExerciseController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExerciseAdvice extends ResponseEntityExceptionHandler {

    // TODO: twoje handlery
}
