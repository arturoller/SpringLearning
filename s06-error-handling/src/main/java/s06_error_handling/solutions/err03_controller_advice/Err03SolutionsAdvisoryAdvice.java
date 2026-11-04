package s06_error_handling.solutions.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import s06_error_handling.err03_controller_advice.ApiError;

@RestControllerAdvice(assignableTypes = Err03SolutionsController.class)
@Order(2)
public class Err03SolutionsAdvisoryAdvice {

    @ExceptionHandler(Err03SolutionsController.ExercisePriorityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError onPriority(HttpServletRequest request) {
        return new ApiError(422, "ADVISORY", "Doradcze", request.getRequestURI());
    }
}
