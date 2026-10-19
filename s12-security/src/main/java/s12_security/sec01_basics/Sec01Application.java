package s12_security.sec01_basics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec01Basics — aplikacja z WŁASNYM łańcuchem filtrów (Sec01SecurityConfig).
 * Uruchom ▶ i wysyłaj zapytania z pliku s12-security/http/sec01_basics.http (część „Własna konfiguracja”).
 * Stan „zaraz po dodaniu startera” pokazuje druga klasa startowa: Sec01StarterOnlyApplication.
 */
@SpringBootApplication
public class Sec01Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec01Application.class, args);
    }
}
