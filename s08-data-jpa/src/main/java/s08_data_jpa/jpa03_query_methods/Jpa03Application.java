package s08_data_jpa.jpa03_query_methods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa03QueryMethods (bez weba). Z jej pakietu Boot skanuje encje ({@code @Entity})
 * i repozytoria Spring Data (interfejsy rozszerzające {@code Repository}). Profil {@code jpa03} wczytuje
 * {@code application-jpa03.properties}: własną bazę H2, Flyway z {@code db/migration/jpa03} i {@code ddl-auto=validate}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jpa03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa03Application.class);
        app.setAdditionalProfiles("jpa03"); // profile = profil ustawień
        app.run(args).close();
    }
}
