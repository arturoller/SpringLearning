package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa rozwiązań lekcji Rest07ApiVersioning — potrzebna testom rozwiązań ({@code @WebMvcTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów). Skanuje tylko pakiet solutions.rest07_api_versioning;
 * wersjonowanie ustawia ten sam profil rest07 co w lekcji.
 */
@SpringBootApplication
public class Rest07SolutionsApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Rest07SolutionsApplication.class).profiles("rest07").run(args);
    }
}
