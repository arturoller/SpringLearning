package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Testy-ĆWICZENIA lekcji Test07DockerCompose: TY piszesz test (usuń linię z UnsupportedOperationException; adnotacje
 * dopisz na klasie — najlepiej każde ćwiczenie w osobnej klasie albo w {@code @Nested}). Ćwiczenia 1, 2 i 5 nie
 * potrzebują Dockera; 3 i 4 — tak (oznacz je {@code @EnabledIfDockerAvailable}).
 * @Tag("cwiczenie") — uruchamiasz sam. Rozwiązania wzorcowe: pakiet s11_testing.solutions.test07_docker_compose.
 */
@Tag("cwiczenie")
class Test07DockerComposeExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: ComposeFile.locate() wskazuje istniejący plik, a w nim jest obraz postgres:17-alpine")
    void exercise1() {
        // TODO: Path file = Path.of(ComposeFile.locate()); assertThat(file).isRegularFile(); Files.readString(file)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: z profilem test07, ale bez skip.in-tests=false, test działa na wbudowanej H2 (adres jdbc:h2:)")
    void exercise2() {
        // TODO: @SpringBootTest(webEnvironment = NONE) + @ActiveProfiles("test07"); DataSource → getMetaData().getURL()
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: z compose — SELECT current_user zwraca 'sklep' (POSTGRES_USER z compose-test07.yaml)")
    void exercise3() {
        // TODO: properties skip.in-tests=false i stop.command=down; @DynamicPropertySource z ComposeFile::locate;
        //       @EnabledIfDockerAvailable; JdbcClient
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: z compose — GET /test07/products/99 zwraca 404 i ProblemDetail z tytułem 'Nie ma takiego produktu'")
    void exercise4() {
        // TODO: webEnvironment = RANDOM_PORT, @LocalServerPort, RestClient; HttpClientErrorException.NotFound
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 5: własny warunek JUnit (ExecutionCondition) — test biegnie tylko, gdy działa polecenie 'docker compose version'")
    void exercise5() {
        // TODO: class DockerComposeCliCondition implements ExecutionCondition { ProcessBuilder("docker", "compose", "version") }
        //       @ExtendWith(DockerComposeCliCondition.class) na klasie testu
        throw new UnsupportedOperationException("TODO");
    }
}
