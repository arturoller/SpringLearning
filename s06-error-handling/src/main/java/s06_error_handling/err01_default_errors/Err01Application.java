package s06_error_handling.err01_default_errors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err01DefaultErrors. Skanuje tylko pakiet s06_error_handling.err01_default_errors, więc
 * nie widzi kontrolerów ani obsługi błędów z innych lekcji. Uruchom ▶ i wyślij zapytania z pliku
 * s06-error-handling/http/err01_default_errors.http.
 */
@SpringBootApplication
public class Err01Application {

    public static void main(String[] args) {
        SpringApplication.run(Err01Application.class, args);
    }
}
