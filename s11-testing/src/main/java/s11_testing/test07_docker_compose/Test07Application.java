package s11_testing.test07_docker_compose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * Klasa startowa lekcji Test07DockerCompose — aplikacja, która przy starcie SAMA uruchamia bazę z pliku
 * {@code s11-testing/compose-test07.yaml} (Docker musi działać).
 * <p>
 * Profil {@code test07} ({@code application-test07.properties}) włącza obsługę Docker Compose i Flyway; ścieżkę do
 * pliku compose ustawia {@link ComposeFile#locate()} jako właściwość domyślną (najsłabszą — każde inne źródło ją
 * nadpisze, np. {@code --spring.docker.compose.file=...}).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s11-testing spring-boot:run -Dspring-boot.run.main-class=s11_testing.test07_docker_compose.Test07Application}.
 * Zapytania: {@code s11-testing/http/test07_docker_compose.http}.
 */
@SpringBootApplication
public class Test07Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test07Application.class);
        app.setAdditionalProfiles("test07");
        app.setDefaultProperties(Map.of("spring.docker.compose.file", ComposeFile.locate())); // default = domyślne
        app.run(args);
    }
}
