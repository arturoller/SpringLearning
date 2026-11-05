package s12_security.sec02_authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec02Authorization. Testy lekcji znajdują ją same (ten sam pakiet).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s12-security spring-boot:run -Dspring-boot.run.main-class=s12_security.sec02_authorization.Sec02Application}.
 * Gotowe zapytania: {@code s12-security/http/sec02_authorization.http}. Użytkownicy (TYLKO DO NAUKI, hasło zawsze
 * {@code haslo-testowe}): {@code anna} i {@code piotr} (CUSTOMER), {@code ewa} (EMPLOYEE + uprawnienie REPORTS_READ),
 * {@code admin} (ADMIN).
 * <p>
 * Lekcja nie ma bazy ani profilu — dane sklepu trzymają serwisy w pamięci, a reguły bezpieczeństwa klasa
 * {@link Sec02SecurityConfig}.
 */
@SpringBootApplication
public class Sec02Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec02Application.class, args);
    }
}
