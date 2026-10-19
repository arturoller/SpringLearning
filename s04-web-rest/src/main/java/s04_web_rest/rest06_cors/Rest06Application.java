package s04_web_rest.rest06_cors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest06Cors. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s04-web-rest/http/rest06_cors.http — z nagłówkiem Origin udajesz przeglądarkę na innej stronie.
 */
@SpringBootApplication
public class Rest06Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest06Application.class, args);
    }
}
