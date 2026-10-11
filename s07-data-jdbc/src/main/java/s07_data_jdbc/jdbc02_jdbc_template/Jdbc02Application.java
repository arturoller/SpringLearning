package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc02JdbcTemplate. Aplikacja BEZ serwera WWW: uruchomiona zbuduje kontekst (z bazą H2
 * i skryptami z {@code db/jdbc02}), wypisze log „Started Jdbc02Application” i się zakończy.
 *
 * <p>Profil {@code jdbc02} (plik {@code application-jdbc02.properties}) włączamy tu w kodzie; w testach robi to
 * {@code @ActiveProfiles("jdbc02")}.</p>
 */
@SpringBootApplication
public class Jdbc02Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Jdbc02Application.class);
        application.setAdditionalProfiles("jdbc02");
        application.run(args);
    }
}
