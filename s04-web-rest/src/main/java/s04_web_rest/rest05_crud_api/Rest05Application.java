package s04_web_rest.rest05_crud_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest05CrudApi. Uruchom ją (▶ obok main) i przejdź po kolei przez zapytania z pliku
 * s04-web-rest/http/rest05_crud_api.http — to pełny cykl życia produktu: utwórz, odczytaj, zastąp, popraw, usuń.
 */
@SpringBootApplication
public class Rest05Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest05Application.class, args);
    }
}
