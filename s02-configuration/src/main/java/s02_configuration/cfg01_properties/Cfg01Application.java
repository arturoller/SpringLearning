package s02_configuration.cfg01_properties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Cfg01Properties. Aplikacja bez serwera WWW: zbuduje kontekst, wypisze log i zakończy się.
 * Włącza profil {@code cfg01}, czyli wczytuje plik {@code application-cfg01.properties} z ustawieniami tej lekcji
 * (profile poznasz w lekcji Cfg03Profiles — tu to tylko sposób, żeby ustawienia lekcji nie wpadły do wspólnego pliku).
 */
@SpringBootApplication
public class Cfg01Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Cfg01Application.class).profiles("cfg01").run(args);
    }
}
