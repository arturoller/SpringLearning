package s07_data_jdbc.jdbc04_flyway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc04Flyway. Aplikacja BEZ serwera WWW: uruchomiona zbuduje kontekst (z bazą H2
 * i migracjami Flyway z {@code db/migration/jdbc04}), wypisze log „Started Jdbc04Application” i się zakończy.
 *
 * <p>Profil {@code jdbc04} (plik {@code application-jdbc04.properties}) włączamy tu w kodzie; w testach robi to
 * {@code @ActiveProfiles("jdbc04")}.</p>
 */
@SpringBootApplication
public class Jdbc04Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Jdbc04Application.class);
        application.setAdditionalProfiles("jdbc04");
        application.run(args);
    }
}
