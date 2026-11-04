package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel05NPlusOne (bez weba). Profil {@code rel05} wczytuje {@code application-rel05.properties}:
 * własną bazę H2, Flyway z {@code db/migration/rel05} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Rel05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel05Application.class);
        app.setAdditionalProfiles("rel05"); // profile = profil ustawień
        app.run(args).close();
    }
}
