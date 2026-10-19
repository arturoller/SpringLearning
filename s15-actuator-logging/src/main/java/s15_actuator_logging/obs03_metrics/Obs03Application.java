package s15_actuator_logging.obs03_metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Obs03Metrics. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s15-actuator-logging/http/obs03_metrics.http. Włącza profil "obs03" (application-obs03.properties).
 */
@SpringBootApplication
public class Obs03Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Obs03Application.class);
        application.setAdditionalProfiles("obs03");   // profil = zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
