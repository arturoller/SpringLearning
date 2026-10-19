package s12_security.sec05_jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec05Jwt. Uruchom ▶ i wysyłaj zapytania z pliku s12-security/http/sec05_jwt.http:
 * najpierw POST /api/auth/token (login i hasło), potem zapytania z nagłówkiem Authorization: Bearer token.
 * Klucze do podpisu są generowane przy starcie — po restarcie stare tokeny są nieważne.
 */
@SpringBootApplication
public class Sec05Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec05Application.class, args);
    }
}
