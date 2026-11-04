package s06_error_handling.solutions.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import s06_error_handling.err03_controller_advice.ApiError;

@RestController
@RequestMapping("/err03/cw/vip")
public class Err03SolutionsVipController {

    @GetMapping("/{id}")
    public String vip(@PathVariable long id) {
        throw new Err03SolutionsController.CustomerNotFoundException(id);
    }

    @ExceptionHandler(Err03SolutionsController.CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onVipNotFound(Err03SolutionsController.CustomerNotFoundException exception, HttpServletRequest request) {
        String id = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
        return new ApiError(404, "VIP_NOT_FOUND", "Nie ma klienta VIP " + id, request.getRequestURI());
    }
}
