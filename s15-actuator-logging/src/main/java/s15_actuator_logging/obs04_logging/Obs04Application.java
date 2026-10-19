package s15_actuator_logging.obs04_logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Obs04Logging. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s15-actuator-logging/http/obs04_logging.http i patrz na konsolę. Włącza profil "obs04".
 */
@SpringBootApplication
public class Obs04Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Obs04Application.class);
        application.setAdditionalProfiles("obs04");   // profil = zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
