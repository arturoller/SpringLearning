package s08_data_jpa.jpa02_repositories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa02Repositories (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data (interfejsy rozszerzające {@code Repository}). Profil {@code jpa02} wczytuje
 * {@code application-jpa02.properties}: własną bazę H2, Flyway z {@code db/migration/jpa02} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jpa02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa02Application.class);
        app.setAdditionalProfiles("jpa02"); // profile = profil ustawień
        app.run(args).close();
    }
}
