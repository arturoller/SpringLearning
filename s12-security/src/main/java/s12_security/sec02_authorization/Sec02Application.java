package s12_security.sec02_authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec02Authorization. Uruchom ▶ i wysyłaj zapytania z pliku
 * s12-security/http/sec02_authorization.http — każde zapytanie jako inny pracownik (anna, piotr, kasia, tomasz).
 */
@SpringBootApplication
public class Sec02Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec02Application.class, args);
    }
}
