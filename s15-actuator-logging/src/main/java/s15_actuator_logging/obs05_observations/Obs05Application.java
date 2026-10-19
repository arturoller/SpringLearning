package s15_actuator_logging.obs05_observations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Obs05Observations. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s15-actuator-logging/http/obs05_observations.http. Włącza profil "obs05" (application-obs05.properties).
 */
@SpringBootApplication
public class Obs05Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Obs05Application.class);
        application.setAdditionalProfiles("obs05");   // profil = zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
