package s04_web_rest.rest06_cors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest06Cors. Skanuje tylko pakiet s04_web_rest.rest06_cors — dlatego CorsConfig (globalne
 * ustawienia CORS) działa TYLKO w tej aplikacji, a nie w innych lekcjach działu. Zapytania: s04-web-rest/http/rest06_cors.http.
 */
@SpringBootApplication
public class Rest06Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest06Application.class, args);
    }
}
