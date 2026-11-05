package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testy-ĆWICZENIA lekcji Test06Testcontainers: TY piszesz test (usuń linię z UnsupportedOperationException; adnotacje
 * Springa dopisz na klasie — najlepiej każde ćwiczenie w osobnej klasie albo w {@code @Nested}).
 * {@code @Testcontainers(disabledWithoutDocker = true)}: bez Dockera ćwiczenia są pomijane — uruchom Docker Desktop.
 * @Tag("cwiczenie") — uruchamiasz sam. Rozwiązania wzorcowe: pakiet s11_testing.solutions.test06_testcontainers.
 */
@Tag("cwiczenie")
@Testcontainers(disabledWithoutDocker = true)
class Test06TestcontainersExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: @DataJpaTest ze wspólnym kontenerem (SharedPostgres) — SELECT version() zaczyna się od 'PostgreSQL 17'")
    void exercise1() {
        // TODO: @DataJpaTest(showSql = false) + @ActiveProfiles("test06") + @ImportTestcontainers(SharedPostgres.class);
        //       jdbc.sql("SELECT version()").query(String.class).single()
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: insertIfAbsent na PostgreSQL — Herbata (już jest) daje 0, Sok daje 1, produktów jest 5")
    void exercise2() {
        // TODO: repository.insertIfAbsent("Herbata", ...) == 0; insertIfAbsent("Sok", ...) == 1; repository.count() == 5
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: na PostgreSQL findTop3MostExpensiveH2Style rzuca wyjątek, a findMostExpensive(3) zwraca Czajnik, Kawa, Kubek")
    void exercise3() {
        // TODO: assertThatThrownBy(...).isInstanceOf(InvalidDataAccessResourceUsageException.class);
        //       extracting(Product::getName).containsExactly("Czajnik", "Kawa", "Kubek")
        //       UWAGA: dwa OSOBNE testy — po błędzie SQL PostgreSQL odrzuca każde kolejne zapytanie w tej transakcji
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: stary sposób — @Container bez @ServiceConnection + @DynamicPropertySource; baza ma 4 produkty z migracji")
    void exercise4() {
        // TODO: static PostgreSQLContainer + registry.add("spring.datasource.url" / "username" / "password", ...)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 5: REST od końca do końca na PostgreSQL — POST Sok → 201, drugi raz → 409, GET /top?n=1 → Czajnik")
    void exercise5() {
        // TODO: @SpringBootTest(webEnvironment = RANDOM_PORT) + @Import(PostgresTestcontainersConfig.class),
        //       @LocalServerPort + RestClient.create("http://localhost:" + port)
        throw new UnsupportedOperationException("TODO");
    }
}
