package s06_error_handling.err03_pitfalls;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ZŁY PRZYKŁAD (tylko do testów): "łapacz wszystkiego" z NAJWYŻSZYM pierwszeństwem. Wyprzedza wszystkie inne advice,
 * więc ich bardziej szczegółowe handlery nigdy się nie wykonują.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GreedyAdvice {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String onAnything(Exception exception) {
        return "zachlanny-500";
    }
}
