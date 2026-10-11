package s08_data_jpa.jpa02_repositories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa02Repositories (profil „jpa02”: baza jdbc:h2:mem:jpa02, wypisywanie SQL).
 * Spring Data znajduje interfejsy repozytoriów w pakiecie tej klasy i tworzy dla nich beany.
 */
@SpringBootApplication
public class Jpa02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa02Application.class);
        app.setAdditionalProfiles("jpa02");
        app.run(args);
    }
}
