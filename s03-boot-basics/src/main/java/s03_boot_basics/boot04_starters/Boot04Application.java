package s03_boot_basics.boot04_starters;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * Klasa startowa lekcji Boot04Starters: wypisuje wersję Boota, technologie obecne na ścieżce klas modułu i to,
 * czy jest bean JsonMapper (od autokonfiguracji ze startera spring-boot-starter-jackson).
 * Bez serwera WWW — w module nie ma startera webowego.
 */
@SpringBootApplication
public class Boot04Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(Boot04Application.class, args)) {
            System.out.println("Spring Boot " + SpringBootVersion.getVersion());
            System.out.println("Technologie na ścieżce klas: " + TechnologyCheck.presentTechnologies());
            System.out.println("Beany JsonMapper: " + String.join(", ", context.getBeanNamesForType(JsonMapper.class)));
        }
    }
}
