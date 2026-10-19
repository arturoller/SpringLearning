package s08_data_jpa.jpa02_repositories;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Jpa02Repositories (profil {@code jpa02}: własna baza H2, Flyway wyłączony, logger SQL).
 * Spring Data znajduje interfejsy repozytoriów w pakiecie tej klasy i tworzy dla nich beany.
 * Brak serwera WWW — aplikacja zbuduje kontekst i zakończy się.
 */
@SpringBootApplication
public class Jpa02Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Jpa02Application.class).profiles("jpa02").run(args);
    }
}
