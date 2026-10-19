package s15_actuator_logging.obs01_actuator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Obs01Actuator. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s15-actuator-logging/http/obs01_actuator.http. Włącza profil "obs01" — ustawienia z pliku application-obs01.properties.
 */
@SpringBootApplication
public class Obs01Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Obs01Application.class);
        application.setAdditionalProfiles("obs01");   // profil = zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
