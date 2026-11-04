package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ćwiczenie 5: ten advice ma wygrywać z {@link ExerciseAdvisoryAdvice}. Dodaj do klasy {@code @Order} z odpowiednią liczbą.
 * Handler jest już gotowy.
 */
@RestControllerAdvice(assignableTypes = ExerciseController.class)
public class ExerciseUrgentAdvice {

    @ExceptionHandler(ExerciseController.ExercisePriorityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onPriority(HttpServletRequest request) {
        return new ApiError(400, "URGENT", "Pilne", request.getRequestURI());
    }
}
