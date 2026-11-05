package s06_error_handling.solutions.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import s06_error_handling.err03_controller_advice.ApiError;

@RestControllerAdvice(assignableTypes = Err03SolutionsController.class)
@Order(1)
public class Err03SolutionsUrgentAdvice {

    @ExceptionHandler(Err03SolutionsController.ExercisePriorityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onPriority(HttpServletRequest request) {
        return new ApiError(400, "URGENT", "Pilne", request.getRequestURI());
    }
}
