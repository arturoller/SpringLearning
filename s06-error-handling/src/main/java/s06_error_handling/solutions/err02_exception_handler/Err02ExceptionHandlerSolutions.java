package s06_error_handling.solutions.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import s06_error_handling.err02_exception_handler.OrderNotFoundException;

import java.util.Map;
import java.util.NoSuchElementException;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err02ExceptionHandler — zajrzyj dopiero po własnej próbie! */
public final class Err02ExceptionHandlerSolutions {

    private Err02ExceptionHandlerSolutions() {
    }

    static HttpStatus solution1(Throwable e) {
        if (e instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;       // także NumberFormatException (podklasa)
        }
        if (e instanceof NoSuchElementException) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @RestController
    static class Solution2Controller {

        @GetMapping("/api/ex/items/{id}")
        Map<String, Object> item(@PathVariable long id) {
            if (id < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id musi być dodatnie");
            }
            if (id > 100) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brak przedmiotu");
            }
            return Map.of("id", id);
        }
    }

    @RestController
    static class Solution3Controller {

        @GetMapping("/api/ex/orders/{id}")
        Map<String, Object> order(@PathVariable long id) {
            if (id == 404) {
                throw new OrderNotFoundException(id);
            }
            return Map.of("id", id);
        }

        @ExceptionHandler(OrderNotFoundException.class)
        ResponseEntity<Map<String, String>> notFound(OrderNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
