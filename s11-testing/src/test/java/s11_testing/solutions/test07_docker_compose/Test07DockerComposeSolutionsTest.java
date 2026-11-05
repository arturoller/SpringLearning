package s11_testing.solutions.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import s11_testing.test07_docker_compose.ComposeFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rozwiązania wzorcowe ćwiczeń Test07DockerCompose. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class Test07DockerComposeSolutionsTest {

    @Nested
    class Exercise1 {

        @Test
        @DisplayName("Wzorzec 1: plik compose istnieje i używa obrazu postgres:17-alpine")
        void composeFile() throws IOException {
            Path file = Path.of(ComposeFile.locate());

            assertThat(file).isRegularFile();
            assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("image: postgres:17-alpine");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test07")
    class Exercise2 {

        @Autowired
        DataSource dataSource;

        @Test
        @DisplayName("Wzorzec 2: bez skip.in-tests=false compose jest pominięty — baza to wbudowana H2")
        void skippedInTests() throws Exception {
            try (var connection = dataSource.getConnection()) {
                assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:");
            }
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "spring.docker.compose.skip.in-tests=false",
            "spring.docker.compose.stop.command=down"
    })
    @ActiveProfiles("test07")
    @EnabledIfDockerAvailable
    class Exercises3And4 {

        @DynamicPropertySource
        static void composeFile(DynamicPropertyRegistry registry) {
            registry.add("spring.docker.compose.file", ComposeFile::locate);
        }

        @Autowired
        JdbcClient jdbc;

        @LocalServerPort
        int port;

        @Test
        @DisplayName("Wzorzec 3: current_user to 'sklep' z compose-test07.yaml")
        void currentUser() {
            assertThat(jdbc.sql("SELECT current_user").query(String.class).single()).isEqualTo("sklep");
        }

        @Test
        @DisplayName("Wzorzec 4: GET /test07/products/99 → 404 z ProblemDetail")
        void notFound() {
            RestClient http = RestClient.create("http://localhost:" + port);

            assertThatThrownBy(() -> http.get().uri("/test07/products/99").retrieve().toBodilessEntity())
                    .isInstanceOfSatisfying(HttpClientErrorException.NotFound.class,
                            e -> assertThat(e.getResponseBodyAsString()).contains("Nie ma takiego produktu"));
        }
    }

    @Nested
    @ExtendWith(DockerComposeCliCondition.class)
    class Exercise5 {

        @Test
        @DisplayName("Wzorzec 5: własny warunek — test biegnie tylko z działającym 'docker compose version'")
        void composeCliAvailable() throws Exception {
            Process process = new ProcessBuilder("docker", "compose", "version").redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            assertThat(process.waitFor()).isZero();
            assertThat(output).contains("Docker Compose");
        }
    }
}
