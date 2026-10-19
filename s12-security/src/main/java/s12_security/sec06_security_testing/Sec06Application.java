package s12_security.sec06_security_testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec06SecurityTesting. Ta lekcja jest o TESTACH — najważniejsze są klasy w
 * src/test/java/s12_security/sec06_security_testing. Aplikację możesz uruchomić (▶) i porównać jej odpowiedzi
 * z testami, wysyłając zapytania z pliku s12-security/http/sec06_security_testing.http.
 */
@SpringBootApplication
public class Sec06Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec06Application.class, args);
    }
}
