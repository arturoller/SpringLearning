package s11_testing.test07_docker_compose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test07DockerCompose. Włącza profil {@code test07} — a z nim obsługę Docker Compose: przy
 * starcie Boot wykonuje „docker compose up” dla pliku s11-testing/compose/test07/compose.yaml, czeka na bazę
 * PostgreSQL i sam podłącza do niej DataSource; przy zamknięciu aplikacji usuwa kontenery („down”).
 * Uruchom ▶ przy main (Docker musi działać; katalog roboczy = s11-testing) i wyślij zapytania
 * z s11-testing/http/test07_docker_compose.http.
 */
@SpringBootApplication
public class Test07Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test07Application.class);
        app.setAdditionalProfiles("test07");
        app.run(args);
    }
}
