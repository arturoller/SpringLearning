package s06_error_handling.err03_controller_advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Kontroler z WŁASNYM handlerem tego samego wyjątku, który obsługuje globalny advice — lokalny wygrywa. */
@RestController
public class LocalOverrideController {

    @GetMapping("/api/local/missing")
    public String missing() {
        throw new ProductNotFoundException(999);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> localNotFound(ProductNotFoundException e) {
        return Map.of("handledBy", "local", "error", "Lokalna obsługa: " + e.getMessage());
    }
}
