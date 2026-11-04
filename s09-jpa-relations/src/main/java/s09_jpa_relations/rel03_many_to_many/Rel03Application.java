package s09_jpa_relations.rel03_many_to_many;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel03ManyToMany (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data. Profil {@code rel03} wczytuje {@code application-rel03.properties}: własną bazę H2,
 * Flyway z {@code db/migration/rel03} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Rel03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel03Application.class);
        app.setAdditionalProfiles("rel03"); // profile = profil ustawień
        app.run(args).close();
    }
}
