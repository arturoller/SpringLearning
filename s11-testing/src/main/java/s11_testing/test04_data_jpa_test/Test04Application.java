package s11_testing.test04_data_jpa_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test04DataJpaTest — przede wszystkim konfiguracja dla testów {@code @DataJpaTest} w pakiecie
 * lekcji. Uruchomiona (▶) włącza profil {@code test04}: baza {@code jdbc:h2:mem:test04}, migracje Flyway
 * z {@code db/migration/test04}. Aplikacja nie ma kontrolerów — „wynikiem” lekcji są testy.
 */
@SpringBootApplication
public class Test04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test04Application.class);
        app.setAdditionalProfiles("test04");
        app.run(args);
    }
}
