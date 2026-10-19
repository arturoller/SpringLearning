package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel04LazyLoading. Włącza profil „rel04” (application-rel04.properties: baza jdbc:h2:mem:rel04,
 * migracje i dane startowe z db/migration/rel04, ddl-auto=validate, statystyki Hibernate). To aplikacja WEBOWA —
 * startuje Tomcata (port 8080) z kontrolerem OrderController; gotowe zapytania: s09-jpa-relations/http/rel04_lazy_loading.http.
 * W logu startu zobaczysz ostrzeżenie o spring.jpa.open-in-view (sekcja 5 lekcji).
 */
@SpringBootApplication
public class Rel04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel04Application.class);
        app.setAdditionalProfiles("rel04");
        app.run(args);
    }
}
