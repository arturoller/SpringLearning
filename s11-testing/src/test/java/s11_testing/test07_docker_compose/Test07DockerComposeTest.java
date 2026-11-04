package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 2–3 lekcji Test07DockerCompose: test, w którym Boot NAPRAWDĘ uruchamia {@code compose-test07.yaml}.
 * <ul>
 *   <li>{@code spring.docker.compose.skip.in-tests=false} — bez tego compose w testach jest pomijany
 *       (Test07SkipInTestsTest),</li>
 *   <li>{@code stop.command=down} — po teście kontener jest USUWANY (w aplikacji profil zostawia domyślne stop),</li>
 *   <li>{@code @EnabledIfDockerAvailable} (z Testcontainers: org.testcontainers.junit.jupiter) — warunek JUnit: bez
 *       Dockera klasa jest POMIJANA. Compose nie ma własnego {@code @Testcontainers}, a ten warunek sprawdza Dockera tak
 *       samo ({@code DockerClientFactory.instance().isDockerAvailable()}).</li>
 * </ul>
 * Ścieżkę do pliku compose podaje {@code @DynamicPropertySource} przez {@link ComposeFile#locate()} — działa i w Maven
 * (katalog roboczy = moduł), i w IntelliJ uruchomionym z katalogu głównego projektu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.docker.compose.skip.in-tests=false",   // skip in tests = pomiń w testach; tu: NIE pomijaj
        "spring.docker.compose.stop.command=down"      // po teście: docker compose down (kontener usunięty)
})
@ActiveProfiles("test07")
@EnabledIfDockerAvailable // enabled if Docker available = włączony, gdy Docker jest dostępny
class Test07DockerComposeTest {

    @DynamicPropertySource
    static void composeFile(DynamicPropertyRegistry registry) {
        registry.add("spring.docker.compose.file", ComposeFile::locate);
    }

    @LocalServerPort
    int port;

    @Autowired
    Environment environment;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcConnectionDetails connectionDetails;

    @Test
    @DisplayName("Sekcja 2: Boot wykrył usługę postgres z compose i sam utworzył połączenie — bez spring.datasource.url")
    void connectionFromCompose() throws Exception {
        assertThat(environment.getProperty("spring.datasource.url")).isNull();               // nikt go nie ustawił
        assertThat(connectionDetails.getClass().getName()).contains("DockerCompose");        // dane połączenia z compose
        assertThat(connectionDetails.getUsername()).isEqualTo("sklep");                       // z POSTGRES_USER

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:postgresql://").contains("/sklep");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(17);
        }
    }

    @Test
    @DisplayName("Sekcja 3: REST na prawdziwym serwerze — aplikacja czyta produkty z PostgreSQL z compose (Flyway V1, V2)")
    void restReadsFromComposeDatabase() {
        RestClient http = RestClient.create("http://localhost:" + port);

        Map<String, String> database = http.get().uri("/test07/database").retrieve()
                .body(new ParameterizedTypeReference<>() { });
        assertThat(database).containsEntry("database", "sklep").containsEntry("user", "sklep");
        assertThat(database.get("version")).startsWith("17.");

        List<Map<String, Object>> products = http.get().uri("/test07/products").retrieve()
                .body(new ParameterizedTypeReference<>() { });
        assertThat(products).extracting(p -> p.get("name")).containsExactly("Czajnik", "Herbata", "Kawa");
    }
}
