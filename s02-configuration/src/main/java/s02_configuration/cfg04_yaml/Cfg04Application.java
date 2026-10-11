package s02_configuration.cfg04_yaml;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Klasa startowa lekcji Cfg04Yaml. Ustawia {@code spring.config.name=cfg04}: Boot szuka wtedy plików
 * {@code cfg04.yml} i {@code cfg04.properties} zamiast {@code application.*} — dzięki temu ustawienia tej lekcji
 * nie mieszają się z innymi lekcjami modułu (które mają wspólną ścieżkę klas).
 * ({@code spring.config.name} to właściwość odczytywana PRZED plikami, więc podajemy ją w kodzie, w argumencie
 * albo w teście — nie wpisuje się jej do samego pliku.)
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Cfg04Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Cfg04Application.class).properties("spring.config.name=cfg04").run(args);
    }
}
