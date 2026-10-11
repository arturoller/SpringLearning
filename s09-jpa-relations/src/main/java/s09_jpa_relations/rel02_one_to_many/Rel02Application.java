package s09_jpa_relations.rel02_one_to_many;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel02OneToMany. Włącza profil „rel02” (application-rel02.properties: baza jdbc:h2:mem:rel02,
 * migracje z db/migration/rel02, ddl-auto=validate). Uruchomiona wykona migracje, sprawdzi encje i zakończy działanie.
 */
@SpringBootApplication
public class Rel02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel02Application.class);
        app.setAdditionalProfiles("rel02");
        app.run(args);
    }
}
