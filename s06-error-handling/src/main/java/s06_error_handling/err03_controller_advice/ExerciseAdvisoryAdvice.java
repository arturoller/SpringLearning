package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Ćwiczenie 5: ten advice ma przegrywać z {@link ExerciseUrgentAdvice}. Dodaj do klasy {@code @Order} z odpowiednią liczbą. */
@RestControllerAdvice(assignableTypes = ExerciseController.class)
public class ExerciseAdvisoryAdvice {

    @ExceptionHandler(ExerciseController.ExercisePriorityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError onPriority(HttpServletRequest request) {
        return new ApiError(422, "ADVISORY", "Doradcze", request.getRequestURI());
    }
}
