package s06_error_handling.err03_controller_advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler z handlerem LOKALNYM dla tego samego wyjątku, który obsługuje advice — lokalny wygrywa (sekcja 2). */
@RestController
@RequestMapping("/err03/local")
public class LocalOverrideController {

    @GetMapping("/products/{id}")
    public Product product(@PathVariable long id) {
        throw new ProductNotFoundException(id);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onProductNotFound(ProductNotFoundException exception, HttpServletRequest request) {
        return new ApiError(404, "LOCAL_NOT_FOUND", "Lokalny handler: " + exception.getMessage(), request.getRequestURI());
    }
}
