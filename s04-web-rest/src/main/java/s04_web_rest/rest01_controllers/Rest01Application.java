package s04_web_rest.rest01_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest01Controllers. Skanuje TYLKO pakiet s04_web_rest.rest01_controllers, więc kontrolery innych
 * lekcji działu (te same adresy /api/products!) nie wchodzą jej w drogę. Uruchom ją (▶ obok main), a potem wysyłaj
 * zapytania z pliku s04-web-rest/http/rest01_controllers.http.
 */
@SpringBootApplication
public class Rest01Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest01Application.class, args);
    }
}
