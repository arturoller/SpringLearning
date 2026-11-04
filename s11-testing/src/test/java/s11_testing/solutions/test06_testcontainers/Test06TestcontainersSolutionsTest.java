package s11_testing.solutions.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import s11_testing.test06_testcontainers.PostgresTestcontainersConfig;
import s11_testing.test06_testcontainers.Product;
import s11_testing.test06_testcontainers.ProductController;
import s11_testing.test06_testcontainers.ProductRepository;
import s11_testing.test06_testcontainers.ProductService;
import s11_testing.test06_testcontainers.SharedPostgres;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rozwiązania wzorcowe ćwiczeń Test06Testcontainers. @Tag("wzorzec") — biegną w domyślnym ./mvnw test
 * (bez Dockera są pomijane dzięki {@code @Testcontainers(disabledWithoutDocker = true)} na każdej klasie z kontenerem).
 */
@Tag("wzorzec")
class Test06TestcontainersSolutionsTest {

    @Nested
    @DataJpaTest(showSql = false)
    @ActiveProfiles("test06")
    @ImportTestcontainers(SharedPostgres.class)
    @Testcontainers(disabledWithoutDocker = true)
    class Exercises1To3 {

        @Autowired
        JdbcClient jdbc;

        @Autowired
        ProductRepository repository;

        @Test
        @DisplayName("Wzorzec 1: wspólny kontener — SELECT version() to PostgreSQL 17")
        void version() {
            assertThat(jdbc.sql("SELECT version()").query(String.class).single()).startsWith("PostgreSQL 17");
        }

        @Test
        @DisplayName("Wzorzec 2: ON CONFLICT — Herbata 0, Sok 1, razem 5")
        void upsert() {
            assertThat(repository.insertIfAbsent("Herbata", new BigDecimal("1.00"))).isZero();
            assertThat(repository.insertIfAbsent("Sok", new BigDecimal("6.99"))).isEqualTo(1);
            assertThat(repository.count()).isEqualTo(5);
        }

        @Test
        @DisplayName("Wzorzec 3a: TOP pada na PostgreSQL")
        void topFails() {
            assertThatThrownBy(() -> repository.findTop3MostExpensiveH2Style())
                    .isInstanceOf(InvalidDataAccessResourceUsageException.class);
        }

        @Test
        @DisplayName("Wzorzec 3b: LIMIT działa (osobny test — po błędzie PostgreSQL odrzuca resztę transakcji testu)")
        void limitWorks() {
            assertThat(repository.findMostExpensive(3)).extracting(Product::getName)
                    .containsExactly("Czajnik", "Kawa", "Kubek");
        }
    }

    @Nested
    @DataJpaTest(showSql = false)
    @ActiveProfiles("test06")
    @Testcontainers(disabledWithoutDocker = true)
    class Exercise4 {

        @Container
        static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

        @DynamicPropertySource
        static void datasource(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        ProductRepository repository;

        @Autowired
        JdbcClient jdbc;

        @Test
        @DisplayName("Wzorzec 4: @DynamicPropertySource — PostgreSQL z kontenera, 4 produkty z migracji")
        void oldStyle() {
            assertThat(jdbc.sql("SELECT current_database()").query(String.class).single()).isEqualTo(postgres.getDatabaseName());
            assertThat(repository.count()).isEqualTo(4);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("test06")
    @Import({ProductService.class, ProductController.class, PostgresTestcontainersConfig.class})
    @Testcontainers(disabledWithoutDocker = true)
    class Exercise5 {

        @LocalServerPort
        int port;

        @Test
        @DisplayName("Wzorzec 5: POST Sok → 201, drugi raz → 409, GET /top?n=1 → Czajnik")
        void endToEnd() {
            RestClient http = RestClient.create("http://localhost:" + port);
            Map<String, Object> body = Map.of("name", "Sok", "price", 6.99);

            assertThat(http.post().uri("/test06/products").contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(201);
            assertThatThrownBy(() -> http.post().uri("/test06/products").contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().toBodilessEntity())
                    .isInstanceOf(HttpClientErrorException.Conflict.class);

            List<Map<String, Object>> top = http.get().uri("/test06/products/top?n=1").retrieve()
                    .body(new ParameterizedTypeReference<>() { });
            assertThat(top).extracting(p -> p.get("name")).containsExactly("Czajnik");
        }
    }
}
