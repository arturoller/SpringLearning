package s11_testing.test06_testcontainers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test06Testcontainers. Profil {@code test06}: H2 w pamięci ({@code jdbc:h2:mem:test06}),
 * Flyway z {@code db/migration/test06}, {@code ddl-auto=validate}.
 * <p>
 * Uruchomiona TAK (▶ obok {@code main}) działa na H2 — POST z {@code http/test06_testcontainers.http} skończy się
 * wtedy błędem 500, bo {@code ON CONFLICT} to składnia PostgreSQL. Z prawdziwym PostgreSQL w kontenerze uruchamiasz
 * ją przez klasę TestTest06Application z {@code src/test/java} (sekcja 5 lekcji):
 * {@code ./mvnw -pl s11-testing spring-boot:test-run -Dspring-boot.run.main-class=s11_testing.test06_testcontainers.TestTest06Application}.
 */
@SpringBootApplication
public class Test06Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test06Application.class);
        app.setAdditionalProfiles("test06");
        app.run(args);
    }
}
