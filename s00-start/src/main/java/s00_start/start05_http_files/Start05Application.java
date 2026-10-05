package s00_start.start05_http_files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Start05HttpFiles. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s00-start/http/start05_http_files.http — ▶ obok każdego zapytania.
 */
@SpringBootApplication
public class Start05Application {

    public static void main(String[] args) {
        SpringApplication.run(Start05Application.class, args);
    }
}
