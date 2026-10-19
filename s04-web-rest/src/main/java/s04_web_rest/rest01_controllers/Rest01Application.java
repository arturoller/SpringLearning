package s04_web_rest.rest01_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest01Controllers. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s04-web-rest/http/rest01_controllers.http. Skanuje tylko pakiet rest01_controllers — inne lekcje działu nie startują.
 */
@SpringBootApplication
public class Rest01Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest01Application.class, args);
    }
}
