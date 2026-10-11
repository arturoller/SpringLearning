package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Zamówienia — drugi kontroler lekcji. Obsługuje WŁASNY wyjątek OrderNotFoundException, ale NIE ma metody dla
 * IllegalArgumentException — a CatalogController ma. Metody {@code @ExceptionHandler} nie "przeciekają" między kontrolerami.
 */
@RestController
public class OrderController {

    @GetMapping("/api/orders/{id}")
    public Map<String, Object> order(@PathVariable long id) {
        if (id == 404) {
            throw new OrderNotFoundException(id);
        }
        return Map.of("id", id, "status", "NOWE");
    }

    /** Rzuca IllegalArgumentException — tu nikt go nie obsługuje (metoda z CatalogController nie działa tutaj). */
    @GetMapping("/api/orders/invalid")
    public String invalid() {
        throw new IllegalArgumentException("Nieprawidłowe zamówienie");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(OrderNotFoundException e) {
        return Map.of("error", e.getMessage());
    }
}
