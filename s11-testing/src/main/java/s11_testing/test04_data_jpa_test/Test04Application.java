package s11_testing.test04_data_jpa_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test04DataJpaTest (bez weba). Z jej pakietu Boot skanuje encje i repozytoria; testy lekcji
 * ({@code @DataJpaTest}) znajdują ją same. Profil {@code test04} wczytuje {@code application-test04.properties}:
 * własną bazę H2, Flyway z {@code db/migration/test04} i {@code ddl-auto=validate}.
 */
@SpringBootApplication
public class Test04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test04Application.class);
        app.setAdditionalProfiles("test04");                 // profile = profil ustawień
        app.setWebApplicationType(WebApplicationType.NONE);  // lekcja nie ma warstwy webowej
        app.run(args).close();
    }
}
