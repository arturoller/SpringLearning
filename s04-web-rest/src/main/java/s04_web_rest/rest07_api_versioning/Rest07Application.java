package s04_web_rest.rest07_api_versioning;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Rest07ApiVersioning. Włącza profil „rest07”, czyli plik application-rest07.properties
 * z ustawieniami spring.mvc.apiversion.* (tylko dla tej lekcji). Uruchom ją (▶ obok main), a potem wysyłaj
 * zapytania z pliku s04-web-rest/http/rest07_api_versioning.http.
 */
@SpringBootApplication
public class Rest07Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Rest07Application.class)
                .profiles("rest07")   // profil lekcji — s02_configuration/Cfg03Profiles
                .run(args);
    }
}
