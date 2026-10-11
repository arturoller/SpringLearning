package s07_data_jdbc.jdbc05_repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc05Repository. Aplikacja BEZ serwera WWW: uruchomiona zbuduje kontekst (z bazą H2
 * i skryptem schematu z {@code db/jdbc05}), wypisze log „Started Jdbc05Application” i się zakończy.
 *
 * <p>Profil {@code jdbc05} (plik {@code application-jdbc05.properties}) włączamy tu w kodzie; w testach robi to
 * {@code @ActiveProfiles("jdbc05")}.</p>
 */
@SpringBootApplication
public class Jdbc05Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Jdbc05Application.class);
        application.setAdditionalProfiles("jdbc05");
        application.run(args);
    }
}
