package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel06EntityToDto (bez weba). Profil {@code rel06} wczytuje
 * {@code application-rel06.properties}: własną bazę H2, Flyway z {@code db/migration/rel06}, {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Rel06Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel06Application.class);
        app.setAdditionalProfiles("rel06"); // profile = profil ustawień
        app.run(args).close();
    }
}
