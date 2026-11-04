package s06_error_handling.err03_controller_advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ćwiczenia 1, 2 i 4: tutaj dopisujesz handlery. Advice jest zawężony do kontrolerów ćwiczeń (assignableTypes) — dzięki temu
 * Twoje handlery nie psują testów lekcji. {@code @Order(LOWEST_PRECEDENCE)} zostaw: ten advice zawiera "łapacz wszystkiego"
 * (ćwiczenie 4), który musi być ostatni (ćwiczenie 5!).
 * <p>
 * Do napisania:
 * <ul>
 *   <li>ćwiczenie 1: {@code CustomerNotFoundException} → 404, code "CUSTOMER_NOT_FOUND", message z wyjątku, path z żądania;</li>
 *   <li>ćwiczenie 2: {@code OrderLockedException} → 409, code "ORDER_LOCKED";</li>
 *   <li>ćwiczenie 4: {@code Exception} → 500, code "UNEXPECTED", message "Wystąpił nieoczekiwany błąd", szczegóły do logu.</li>
 * </ul>
 * Format ciała: rekord {@link ApiError}.
 */
@RestControllerAdvice(assignableTypes = {ExerciseController.class, ExerciseVipController.class})
@Order(Ordered.LOWEST_PRECEDENCE)
public class ExerciseAdvice {

    // TODO: twoje handlery
}
