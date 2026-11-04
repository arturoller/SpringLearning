package s12_security.sec01_basics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec01Basics. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same, bo leżą
 * w tym samym pakiecie.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s12-security spring-boot:run -Dspring-boot.run.main-class=s12_security.sec01_basics.Sec01Application}.
 * Gotowe zapytania: {@code s12-security/http/sec01_basics.http}. Użytkownicy (TYLKO DO NAUKI): {@code anna} / {@code haslo-testowe}
 * (rola CUSTOMER) i {@code admin} / {@code haslo-testowe} (rola ADMIN).
 * <p>
 * Ta lekcja nie potrzebuje własnego profilu: nie ma bazy ani migracji, a całą konfigurację bezpieczeństwa trzyma klasa
 * {@link Sec01SecurityConfig}. Skanowanie obejmuje tylko pakiet {@code sec01_basics}, więc reguły tej lekcji nie działają
 * w innych lekcjach działu.
 */
@SpringBootApplication
public class Sec01Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec01Application.class, args);
    }
}
