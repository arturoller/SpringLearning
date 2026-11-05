package s06_error_handling.err05_custom_exceptions;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Ćwiczenia 1-5: tutaj dopisujesz handlery. Advice jest zawężony do {@link ExerciseController} (assignableTypes) i ma najwyższe
 * pierwszeństwo, więc nie koliduje z {@code Err05Advice} z lekcji. Dziedziczy po {@code ResponseEntityExceptionHandler}
 * (Err04ProblemDetail). Wszystkie odpowiedzi to {@code ProblemDetail}.
 * <p>
 * Do napisania (typ wyjątku → odpowiedź):
 * <ul>
 *   <li>ćwiczenie 1: {@code ExerciseNotFoundException} → 404, title "Nie znaleziono zasobu", detail z wyjątku, code z wyjątku,
 *       type = "https://sklep.example/errors/" + kod małymi literami z myślnikami (np. customer-not-found);</li>
 *   <li>ćwiczenie 2: {@code ExerciseRuleException} → 422, title "Naruszona reguła biznesowa", reszta jak wyżej;</li>
 *   <li>ćwiczenie 3: {@code InvoiceAlreadyPaidException} → 409 (title "Konflikt ze stanem sklepu"); pozostałe reguły nadal 422;</li>
 *   <li>ćwiczenie 4: {@code Exception} → 500, detail "Wystąpił nieoczekiwany błąd", code "UNEXPECTED", szczegóły do logu;</li>
 *   <li>ćwiczenie 5: {@code WarehouseBusyException} → 503 z nagłówkiem Retry-After i właściwością retryAfterSeconds.</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = ExerciseController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExerciseAdvice extends ResponseEntityExceptionHandler {

    // TODO: twoje handlery
}
