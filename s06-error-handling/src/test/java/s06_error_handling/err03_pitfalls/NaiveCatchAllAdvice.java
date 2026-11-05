package s06_error_handling.err03_pitfalls;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ZŁY PRZYKŁAD (leży w katalogu testów, w pakiecie spoza skanowania lekcji, i jest włączany tylko przez {@code @Import}
 * w teście pułapek Err03PitfallsTest): "łapacz wszystkiego", który zawsze zwraca 500. Domyślne pierwszeństwo, bez ostrzeżeń.
 */
@RestControllerAdvice
public class NaiveCatchAllAdvice {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String onAnything(Exception exception) {
        return "naiwny-500";
    }
}
