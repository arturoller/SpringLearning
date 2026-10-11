package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc03JdbcTemplate. Aplikacja BEZ serwera WWW: uruchomiona zbuduje kontekst (z bazą H2
 * i skryptami z {@code db/jdbc03}), wypisze log „Started Jdbc03Application” i się zakończy.
 *
 * <p>Profil {@code jdbc03} (plik {@code application-jdbc03.properties}) włączamy tu w kodzie; w testach robi to
 * {@code @ActiveProfiles("jdbc03")}.</p>
 */
@SpringBootApplication
public class Jdbc03Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Jdbc03Application.class);
        application.setAdditionalProfiles("jdbc03");
        application.run(args);
    }
}
