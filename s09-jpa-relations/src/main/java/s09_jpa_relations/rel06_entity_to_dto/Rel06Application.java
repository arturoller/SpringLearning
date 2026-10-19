package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel06EntityToDto. Włącza profil „rel06” (application-rel06.properties: baza jdbc:h2:mem:rel06,
 * migracje i dane startowe z db/migration/rel06, ddl-auto=validate, open-in-view=false). To aplikacja WEBOWA —
 * startuje Tomcata (port 8080); gotowe zapytania: s09-jpa-relations/http/rel06_entity_to_dto.http.
 */
@SpringBootApplication
public class Rel06Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel06Application.class);
        app.setAdditionalProfiles("rel06");
        app.run(args);
    }
}
