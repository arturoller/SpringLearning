package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 3–4 lekcji Test06Testcontainers: CAŁA aplikacja ({@code @SpringBootTest(RANDOM_PORT)}) na PostgreSQL
 * z kontenera-beana ({@code @Import(PostgresTestcontainersConfig.class)}). Żądania HTTP idą przez prawdziwy serwer
 * (Tomcat na losowym porcie), kontroler, serwis i repozytorium aż do PostgreSQL — test „od końca do końca”.
 * Klasa nie ma pól {@code @Container}, ale {@code @Testcontainers(disabledWithoutDocker = true)} i tak ją pomija
 * bez Dockera.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test06")
@Import(PostgresTestcontainersConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class Test06SpringBootPostgresTest {

    @LocalServerPort // port, na którym wstał serwer testu
    int port;

    @Autowired
    PostgreSQLContainer postgres; // kontener jest zwykłym beanem — można go wstrzyknąć

    @Autowired
    ProductRepository repository;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.create("http://localhost:" + port);
        repository.findAll().stream().filter(p -> p.getId() > 4).forEach(repository::delete); // sprzątanie: zostają dane z migracji
    }

    @Test
    @DisplayName("Sekcja 4: kontener z @Bean @ServiceConnection działa i jest beanem kontekstu")
    void containerIsBean() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getDockerImageName()).isEqualTo("postgres:17-alpine");
    }

    @Test
    @DisplayName("Sekcja 3: POST nowego produktu → 201, ten sam produkt drugi raz → 409 Conflict (ON CONFLICT na PostgreSQL)")
    void postTwice() {
        Map<String, Object> body = Map.of("name", "Ciastka", "price", 12.50);

        ResponseEntity<Void> created = http.post().uri("/test06/products").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
        assertThat(created.getStatusCode().value()).isEqualTo(201);

        assertThatThrownBy(() -> http.post().uri("/test06/products").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity())
                .isInstanceOfSatisfying(HttpClientErrorException.Conflict.class,
                        e -> assertThat(e.getResponseBodyAsString()).contains("Produkt 'Ciastka' już istnieje"));
    }

    @Test
    @DisplayName("Sekcja 3: GET /test06/products/top?n=2 → Czajnik i Kawa (natywne LIMIT na PostgreSQL)")
    void topTwo() {
        List<Map<String, Object>> top = http.get().uri("/test06/products/top?n=2").retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() { });

        assertThat(top).extracting(p -> p.get("name")).containsExactly("Czajnik", "Kawa");
    }
}
