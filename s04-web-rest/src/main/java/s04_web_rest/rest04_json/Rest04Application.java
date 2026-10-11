package s04_web_rest.rest04_json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest04Json. Uruchom ją (▶ obok main) i wysyłaj zapytania z pliku
 * s04-web-rest/http/rest04_json.http.
 */
@SpringBootApplication
public class Rest04Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest04Application.class, args);
    }
}
