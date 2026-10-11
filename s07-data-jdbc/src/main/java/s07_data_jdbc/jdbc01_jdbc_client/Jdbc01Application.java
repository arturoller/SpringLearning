package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc01JdbcClient. Aplikacja BEZ serwera WWW: uruchomiona zbuduje kontekst (z bazą H2
 * i skryptami z {@code db/jdbc01}), wypisze log „Started Jdbc01Application” i się zakończy.
 *
 * <p>Profil {@code jdbc01} (plik {@code application-jdbc01.properties}) włączamy tu w kodzie; w testach robi to
 * {@code @ActiveProfiles("jdbc01")}.</p>
 */
@SpringBootApplication
public class Jdbc01Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Jdbc01Application.class);
        application.setAdditionalProfiles("jdbc01");
        application.run(args);
    }
}
