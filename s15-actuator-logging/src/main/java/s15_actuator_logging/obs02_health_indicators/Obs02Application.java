package s15_actuator_logging.obs02_health_indicators;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Obs02HealthIndicators. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s15-actuator-logging/http/obs02_health_indicators.http. Włącza profil "obs02" (application-obs02.properties).
 */
@SpringBootApplication
public class Obs02Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Obs02Application.class);
        application.setAdditionalProfiles("obs02");   // profil = zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
