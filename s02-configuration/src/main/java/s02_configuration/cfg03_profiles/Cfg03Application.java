package s02_configuration.cfg03_profiles;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Cfg03Profiles. Uruchamiana z IntelliJ, włącza profile {@code cfg03} i {@code cfg03-dev}
 * (można je zmienić argumentem {@code --spring.profiles.active=cfg03,cfg03-prod}). Brak serwera WWW —
 * aplikacja zbuduje kontekst, wypisze log i zakończy się.
 */
@SpringBootApplication
public class Cfg03Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Cfg03Application.class).profiles("cfg03", "cfg03-dev").run(args);
    }
}
