package s11_testing.test06_testcontainers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test06Testcontainers (profil {@code test06}: schemat i dane klientów z plików SQL).
 * Uruchomiona tak (▶ przy main) rozmawia z bazą H2 w pamięci. Z PRAWDZIWYM PostgreSQL w kontenerze uruchamia ją
 * klasa {@code Test06DevApplication} z src/test/java (sekcja 5 lekcji) — potrzebny działający Docker.
 */
@SpringBootApplication
public class Test06Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test06Application.class);
        app.setAdditionalProfiles("test06");
        app.run(args);
    }
}
