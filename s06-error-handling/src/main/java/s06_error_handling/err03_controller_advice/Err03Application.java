package s06_error_handling.err03_controller_advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err03ControllerAdvice. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s06-error-handling/http/err03_controller_advice.http.
 */
@SpringBootApplication
public class Err03Application {

    public static void main(String[] args) {
        SpringApplication.run(Err03Application.class, args);
    }
}
