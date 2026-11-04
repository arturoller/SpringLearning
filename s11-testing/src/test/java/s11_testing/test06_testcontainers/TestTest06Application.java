package s11_testing.test06_testcontainers;

import org.springframework.boot.SpringApplication;

/**
 * Uruchamia aplikację lekcji Test06Testcontainers LOKALNIE z prawdziwym PostgreSQL w kontenerze (sekcja 5 lekcji).
 * Leży w {@code src/test/java}, bo korzysta z bibliotek testowych (Testcontainers) — do paczki produkcyjnej nie trafia.
 * <p>
 * {@code SpringApplication.from(Test06Application::main)} — „uruchom zwykłą metodę main aplikacji”,
 * {@code .with(PostgresTestcontainersConfig.class)} — „i dołóż tę konfigurację” (kontener jako bean z
 * {@code @ServiceConnection}). Kontener startuje razem z aplikacją i znika po jej zatrzymaniu.
 * <p>
 * PUŁAPKA: (sprawdzona) bez {@code .withAdditionalProfiles("test06")} profil ustawiony w {@code Test06Application.main}
 * ({@code app.setAdditionalProfiles("test06")}) GINIE — mechanizm {@code from(...)} nadpisuje listę dodatkowych
 * profili swoją (pustą). Aplikacja wstaje z profilem "default", Flyway jest wyłączony, a pierwsze zapytanie kończy się
 * błędem 500: {@code relation "product" does not exist}.
 * <p>
 * Uruchomienie: IntelliJ — ▶ obok {@code main} w TEJ klasie; wiersz poleceń (Docker musi działać):
 * {@code ./mvnw -pl s11-testing spring-boot:test-run -Dspring-boot.run.main-class=s11_testing.test06_testcontainers.TestTest06Application}.
 * Zapytania: {@code s11-testing/http/test06_testcontainers.http}.
 */
public class TestTest06Application {

    public static void main(String[] args) {
        SpringApplication.from(Test06Application::main) // from = z (metody main aplikacji)
                .with(PostgresTestcontainersConfig.class)   // with = z dodatkiem (konfiguracji testowej)
                .withAdditionalProfiles("test06")            // KONIECZNE — patrz PUŁAPKA wyżej
                .run(args);
    }
}
