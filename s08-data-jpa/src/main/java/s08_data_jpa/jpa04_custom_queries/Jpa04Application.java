package s08_data_jpa.jpa04_custom_queries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa04CustomQueries (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data. Profil {@code jpa04} wczytuje {@code application-jpa04.properties}: własną bazę H2,
 * Flyway z {@code db/migration/jpa04} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jpa04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa04Application.class);
        app.setAdditionalProfiles("jpa04"); // profile = profil ustawień
        app.run(args).close();
    }
}
