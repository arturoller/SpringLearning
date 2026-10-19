package s12_security.sec04_stateless_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec04StatelessApi. Uruchom ▶ i wysyłaj zapytania z pliku
 * s12-security/http/sec04_stateless_api.http. Część „web” (formularz, sesja, CSRF) najwygodniej obejrzeć
 * w przeglądarce: http://localhost:8080/web/profile.
 */
@SpringBootApplication
public class Sec04Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec04Application.class, args);
    }
}
