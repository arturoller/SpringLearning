package s14_openapi.api02_documenting_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Api02DocumentingApi. Uruchom ją (▶ obok main), otwórz http://localhost:8080/swagger-ui.html
 * i porównaj z dokumentem z lekcji Api01Springdoc — albo wysyłaj zapytania z pliku
 * s14-openapi/http/api02_documenting_api.http.
 */
@SpringBootApplication
public class Api02Application {

    public static void main(String[] args) {
        SpringApplication.run(Api02Application.class, args);
    }
}
