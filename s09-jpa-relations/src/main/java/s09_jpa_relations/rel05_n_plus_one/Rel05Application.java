package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel05NPlusOne. Włącza profil „rel05” (application-rel05.properties: baza jdbc:h2:mem:rel05,
 * migracje i dane startowe z db/migration/rel05, ddl-auto=validate, statystyki Hibernate, bez serwera WWW).
 * Uruchomiona wykona migracje, sprawdzi encje i zakończy działanie — lekcję „oglądasz” w testach.
 */
@SpringBootApplication
public class Rel05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel05Application.class);
        app.setAdditionalProfiles("rel05");
        app.run(args);
    }
}
