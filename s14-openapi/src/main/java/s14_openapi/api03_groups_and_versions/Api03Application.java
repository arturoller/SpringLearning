package s14_openapi.api03_groups_and_versions;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Api03GroupsAndVersions. Włącza profil „api03” (plik application-api03.properties z ustawieniami
 * wersjonowania API). Uruchom ją (▶ obok main), otwórz http://localhost:8080/swagger-ui.html albo wysyłaj zapytania z pliku
 * s14-openapi/http/api03_groups_and_versions.http.
 */
@SpringBootApplication
public class Api03Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Api03Application.class)
                .profiles("api03")   // profil lekcji — s02_configuration/Cfg03Profiles
                .run(args);
    }
}
