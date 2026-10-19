package s14_openapi.api01_springdoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Api01Springdoc. Uruchom ją (▶ obok main), a potem otwórz w przeglądarce
 * http://localhost:8080/swagger-ui.html albo wysyłaj zapytania z pliku s14-openapi/http/api01_springdoc.http.
 */
@SpringBootApplication
public class Api01Application {

    public static void main(String[] args) {
        SpringApplication.run(Api01Application.class, args);
    }
}
