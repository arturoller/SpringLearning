package s02_configuration.cfg02_configuration_properties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Klasa startowa lekcji Cfg02ConfigurationProperties. {@code @ConfigurationPropertiesScan} (scan = skanowanie) szuka
 * klas z {@code @ConfigurationProperties} w pakiecie tej klasy (i podpakietach) i rejestruje je jako beany —
 * dzięki temu nie trzeba pisać {@code @EnableConfigurationProperties} dla każdej z osobna.
 * Skanowanie ogranicza się do pakietu lekcji, więc nie dotyka innych lekcji modułu.
 * Profil {@code cfg02} wczytuje plik application-cfg02.properties.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Cfg02Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Cfg02Application.class).profiles("cfg02").run(args);
    }
}
