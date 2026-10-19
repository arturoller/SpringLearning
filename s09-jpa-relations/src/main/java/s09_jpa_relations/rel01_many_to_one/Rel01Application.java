package s09_jpa_relations.rel01_many_to_one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel01ManyToOne. Włącza profil „rel01” (plik application-rel01.properties: własna baza H2
 * jdbc:h2:mem:rel01, migracje Flyway z db/migration/rel01, ddl-auto=validate, wypisywanie SQL). Uruchomiona wykona
 * migracje, sprawdzi encje względem tabel i zakończy działanie. Moduł ma co prawda Spring MVC (dla lekcji Rel04 i Rel06),
 * ale profil rel01 ustawia {@code spring.main.web-application-type=none} — ta lekcja nie startuje serwera WWW.
 */
@SpringBootApplication
public class Rel01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel01Application.class);
        app.setAdditionalProfiles("rel01");
        app.run(args);
    }
}
