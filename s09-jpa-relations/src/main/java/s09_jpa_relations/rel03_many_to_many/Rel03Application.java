package s09_jpa_relations.rel03_many_to_many;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel03ManyToMany. Włącza profil „rel03” (application-rel03.properties: baza jdbc:h2:mem:rel03,
 * migracje z db/migration/rel03, ddl-auto=validate, statystyki Hibernate). Uruchomiona wykona migracje i się zakończy.
 */
@SpringBootApplication
public class Rel03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel03Application.class);
        app.setAdditionalProfiles("rel03");
        app.run(args);
    }
}
