package s04_web_rest.rest02_request_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest02RequestData. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s04-web-rest/http/rest02_request_data.http.
 */
@SpringBootApplication
public class Rest02Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest02Application.class, args);
    }
}
