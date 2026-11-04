package s04_web_rest.rest07_api_versioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest07ApiVersioning. Włącza profil {@code rest07}, więc wczytuje
 * {@code application-rest07.properties} z ustawieniami wersjonowania (spring.mvc.apiversion.*). Inne lekcje działu
 * tego profilu nie włączają, więc tych ustawień nie dostają.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest07_api_versioning.Rest07Application}.
 * Gotowe zapytania: {@code s04-web-rest/http/rest07_api_versioning.http}.
 */
@SpringBootApplication
public class Rest07Application {

    /** Nazwa profilu lekcji (profile = profil: zestaw ustawień włączany na żądanie, patrz s02_configuration/Cfg03Profiles). */
    public static final String PROFILE = "rest07";

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Rest07Application.class);
        application.setAdditionalProfiles(PROFILE);
        application.run(args);
    }
}
