package s21_capstone.cap02_reservations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa projektu Cap02Reservations. Włącza profil „cap02” (baza jdbc:h2:mem:cap02, migracje
 * db/migration/cap02, ZATRZYMANY zegar 2026-10-19 10:00). Scenariusz: s21-capstone/http/cap02_reservations.http.
 */
@SpringBootApplication
public class Cap02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Cap02Application.class);
        app.setAdditionalProfiles("cap02");
        app.run(args);
    }
}
