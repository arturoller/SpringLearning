package s02_configuration.cfg05_environment;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Cfg05Environment. Włącza profil {@code cfg05} (plik application-cfg05.properties).
 * Aplikacja bez serwera WWW — buduje kontekst i kończy działanie.
 */
@SpringBootApplication
public class Cfg05Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Cfg05Application.class).profiles("cfg05").run(args);
    }
}
