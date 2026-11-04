package s09_jpa_relations.rel01_many_to_one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel01ManyToOne (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data. Profil {@code rel01} wczytuje {@code application-rel01.properties}: własną bazę H2,
 * Flyway z {@code db/migration/rel01} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Rel01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel01Application.class);
        app.setAdditionalProfiles("rel01"); // profile = profil ustawień
        app.run(args).close();
    }
}
