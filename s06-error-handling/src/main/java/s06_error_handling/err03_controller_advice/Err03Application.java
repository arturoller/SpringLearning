package s06_error_handling.err03_controller_advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err03ControllerAdvice. Skanuje tylko pakiet s06_error_handling.err03_controller_advice — dlatego advice (porada) z tej lekcji
 * działa wyłącznie w jej aplikacji i nie wpływa na inne lekcje. Zapytania: s06-error-handling/http/err03_controller_advice.http.
 */
@SpringBootApplication
public class Err03Application {

    public static void main(String[] args) {
        SpringApplication.run(Err03Application.class, args);
    }
}
