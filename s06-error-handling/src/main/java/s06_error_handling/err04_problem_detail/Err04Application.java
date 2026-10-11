package s06_error_handling.err04_problem_detail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err04ProblemDetail. Własny advice lekcji włącza się właściwością
 * {@code err04.custom-advice=true} (domyślnie wyłączony, żeby można było pokazać też ustawienia Boota bez niego).
 * Aby uruchomić z advice: argument programu {@code --err04.custom-advice=true}.
 * Zapytania: s06-error-handling/http/err04_problem_detail.http.
 */
@SpringBootApplication
public class Err04Application {

    public static void main(String[] args) {
        SpringApplication.run(Err04Application.class, args);
    }
}
