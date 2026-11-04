package s12_security.sec04_stateless_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec04StatelessApi. Testy lekcji znajdują ją same (ten sam pakiet).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s12-security spring-boot:run -Dspring-boot.run.main-class=s12_security.sec04_stateless_api.Sec04Application}.
 * Gotowe zapytania: {@code s12-security/http/sec04_stateless_api.http}. Użytkownicy (TYLKO DO NAUKI, hasło zawsze
 * {@code haslo-testowe}): {@code anna} (CUSTOMER), {@code ewa} (EMPLOYEE), {@code admin} (ADMIN).
 * <p>
 * Aplikacja ma DWA łańcuchy filtrów ({@link Sec04SecurityConfig}): bezstanowe API pod {@code /sec04/api/**} i "stronę dla
 * przeglądarki" z sesją i ochroną CSRF pod {@code /sec04/web/**}.
 */
@SpringBootApplication
public class Sec04Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec04Application.class, args);
    }
}
