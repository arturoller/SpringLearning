package s06_error_handling.err04_problem_detail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err04ProblemDetail. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s06-error-handling/http/err04_problem_detail.http.
 */
@SpringBootApplication
public class Err04Application {

    public static void main(String[] args) {
        SpringApplication.run(Err04Application.class, args);
    }
}
