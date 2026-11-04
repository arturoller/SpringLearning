package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1–3 lekcji Test06Testcontainers: {@code @DataJpaTest} na PRAWDZIWYM PostgreSQL 17 w kontenerze.
 * <ul>
 *   <li>{@code @Testcontainers(disabledWithoutDocker = true)} — rozszerzenie JUnit Testcontainers: uruchamia
 *       kontenery z pól {@code @Container}, a bez Dockera POMIJA całą klasę (zamiast czerwonego błędu).</li>
 *   <li>{@code @Container static} — jeden kontener na klasę: start przed pierwszym testem, stop po ostatnim.</li>
 *   <li>{@code @ServiceConnection} — Boot sam tworzy połączenie z adresu, loginu i hasła kontenera (bez
 *       {@code spring.datasource.*}). Domyślne {@code @AutoConfigureTestDatabase(replace = NON_TEST)} NIE podmienia
 *       takiej bazy na H2 — więc {@code replace = NONE} ze starych poradników nie jest już potrzebne.</li>
 * </ul>
 * Flyway lekcji (profil test06) wykonuje się na PostgreSQL — te same migracje co na H2.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test06")
@Testcontainers(disabledWithoutDocker = true) // bez Dockera: klasa pominięta, build zielony
class Test06ServiceConnectionTest {

    @Container        // Testcontainers uruchamia i zatrzymuje kontener
    @ServiceConnection // Boot bierze z niego dane połączenia
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    ProductRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("Sekcja 2: @ServiceConnection — wycinek łączy się z PostgreSQL 17 z kontenera (nie z H2), mimo braku replace = NONE")
    void connectedToPostgres() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            assertThat(meta.getURL()).isEqualTo(postgres.getJdbcUrl()).startsWith("jdbc:postgresql://");
            assertThat(meta.getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(meta.getDatabaseMajorVersion()).isEqualTo(17);
        }
    }

    @Test
    @DisplayName("Sekcja 3: Flyway lekcji wykonał V1 i V2 na PostgreSQL — 4 produkty z migracji")
    void flywayRanOnPostgres() {
        assertThat(jdbc.sql("SELECT version FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank")
                .query(String.class).list()).containsExactly("1", "2");
        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("Sekcja 1: na PostgreSQL 'ON CONFLICT DO NOTHING' działa — 0 dla istniejącej nazwy, 1 dla nowej")
    void upsertWorksOnPostgres() {
        assertThat(repository.insertIfAbsent("Kawa", new BigDecimal("1.00"))).isZero();
        assertThat(repository.insertIfAbsent("Ciastka", new BigDecimal("12.50"))).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sekcja 1: na PostgreSQL 'SELECT TOP 3' (zielone na H2!) to błąd składni")
    void h2SyntaxFailsOnPostgres() {
        assertThatThrownBy(() -> repository.findTop3MostExpensiveH2Style())
                .isInstanceOf(InvalidDataAccessResourceUsageException.class)
                .hasMessageContaining("syntax error");
    }

    @Test
    @DisplayName("Sekcja 1: PostgreSQL po błędzie SQL PRZERYWA transakcję — kolejne zapytanie w niej też pada ('current transaction is aborted')")
    void postgresAbortsTransactionAfterError() {
        assertThatThrownBy(() -> repository.findTop3MostExpensiveH2Style())
                .isInstanceOf(InvalidDataAccessResourceUsageException.class);

        assertThatThrownBy(() -> repository.findMostExpensive(2)) // poprawne zapytanie, ta sama transakcja testu
                .hasMessageContaining("current transaction is aborted");
    }

    @Test
    @DisplayName("Sekcja 1: przenośne 'LIMIT' działa także na PostgreSQL — ten sam wynik co na H2")
    void portableSyntaxWorksOnPostgres() {
        assertThat(repository.findMostExpensive(2)).extracting(Product::getName).containsExactly("Czajnik", "Kawa");
    }
}
