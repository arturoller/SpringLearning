package s06_error_handling.err05_custom_exceptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err05CustomExceptions. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s06-error-handling/http/err05_custom_exceptions.http.
 */
@SpringBootApplication
public class Err05Application {

    public static void main(String[] args) {
        SpringApplication.run(Err05Application.class, args);
    }
}
