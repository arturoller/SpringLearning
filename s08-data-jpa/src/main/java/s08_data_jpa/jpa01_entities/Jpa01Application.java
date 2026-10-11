package s08_data_jpa.jpa01_entities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa01Entities. Włącza profil „jpa01” (plik application-jpa01.properties: własna baza H2
 * jdbc:h2:mem:jpa01 i wypisywanie SQL). Uruchomiona tworzy tabele z encji (w logu widać create table ...)
 * i kończy działanie — moduł nie ma serwera WWW.
 */
@SpringBootApplication
public class Jpa01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa01Application.class);
        app.setAdditionalProfiles("jpa01");
        app.run(args);
    }
}
