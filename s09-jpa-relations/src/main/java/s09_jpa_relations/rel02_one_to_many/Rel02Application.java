package s09_jpa_relations.rel02_one_to_many;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel02OneToMany (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data. Profil {@code rel02} wczytuje {@code application-rel02.properties}: własną bazę H2,
 * Flyway z {@code db/migration/rel02} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Rel02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel02Application.class);
        app.setAdditionalProfiles("rel02"); // profile = profil ustawień
        app.run(args).close();
    }
}
