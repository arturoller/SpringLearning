package s12_security.sec06_security_testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec06SecurityTesting — aplikacja, którą TESTUJEMY (lekcja jest o testach, kod aplikacji to powtórka
 * Sec02–Sec05). Testy lekcji znajdują ją same (ten sam pakiet).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s12-security spring-boot:run -Dspring-boot.run.main-class=s12_security.sec06_security_testing.Sec06Application}.
 * Gotowe zapytania: {@code s12-security/http/sec06_security_testing.http}. Użytkownicy (TYLKO DO NAUKI, hasło zawsze
 * {@code haslo-testowe}): {@code anna} i {@code piotr} (CUSTOMER), {@code ewa} (EMPLOYEE), {@code admin} (ADMIN).
 */
@SpringBootApplication
public class Sec06Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec06Application.class, args);
    }
}
