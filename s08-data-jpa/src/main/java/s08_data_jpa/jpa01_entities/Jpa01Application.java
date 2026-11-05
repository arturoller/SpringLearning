package s08_data_jpa.jpa01_entities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa01Entities (bez weba). Pakiet tej klasy wyznacza, gdzie Boot szuka encji
 * (skanowanie {@code @Entity}) — tylko {@code jpa01_entities} i podpakiety, więc encje innych lekcji tu nie trafiają.
 * Profil {@code jpa01} wczytuje {@code application-jpa01.properties}: własną bazę H2 i {@code ddl-auto=create-drop}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jpa01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa01Application.class);
        app.setAdditionalProfiles("jpa01"); // profile = profil ustawień
        app.run(args).close();
    }
}
