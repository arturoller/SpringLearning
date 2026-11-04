package s08_data_jpa.jpa06_flyway_with_jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa06FlywayWithJpa (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data. Profil {@code jpa06} wczytuje {@code application-jpa06.properties}: własną bazę H2,
 * Flyway z {@code db/migration/jpa06} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jpa06Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa06Application.class);
        app.setAdditionalProfiles("jpa06"); // profile = profil ustawień
        app.run(args).close();
    }
}
