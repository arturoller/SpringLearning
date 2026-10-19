package s21_capstone.cap03_library_with_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa projektu Cap03LibraryWithAuth. Włącza profil „cap03” (baza jdbc:h2:mem:cap03, migracje
 * db/migration/cap03, zasady biblioteki, ZATRZYMANY zegar 2026-10-19 10:00). Klucze JWT powstają przy starcie.
 * Scenariusz z logowaniem: s21-capstone/http/cap03_library_with_auth.http.
 */
@SpringBootApplication
public class Cap03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Cap03Application.class);
        app.setAdditionalProfiles("cap03");
        app.run(args);
    }
}
