package s08_data_jpa.jpa06_flyway_with_jpa;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Jpa06FlywayWithJpa: scenariusz V1 → V2 → V3 krok po kroku, bez Springa — samo API Flyway
 * ({@code Flyway.configure()…migrate()}) na własnej bazie H2. {@code target} = do której wersji migrować.
 */
class Jpa06MigrationVersionsTest {

    private final String url = "jdbc:h2:mem:jpa06-versions-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    private final JdbcClient jdbc = JdbcClient.create(new DriverManagerDataSource(url, "sa", ""));

    @AfterEach
    void dropDatabase() {
        jdbc.sql("SHUTDOWN").update(); // DB_CLOSE_DELAY=-1 trzyma bazę w pamięci — sprzątamy ręcznie
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration/jpa06")
                .target(target)
                .load();
    }

    private List<String> productColumns() {
        return jdbc.sql("SELECT LOWER(column_name) FROM information_schema.columns WHERE table_name = 'PRODUCT' "
                + "ORDER BY ordinal_position").query(String.class).list();
    }

    @Test
    @DisplayName("Sekcja 4: do V2 tabela nie ma weight_grams; V3 dodaje kolumnę z DEFAULT 0 i uzupełnia znane wagi")
    void fromV2ToV3() {
        flyway("2").migrate();
        assertThat(productColumns()).doesNotContain("weight_grams").contains("name", "stock");
        assertThat(jdbc.sql("SELECT count(*) FROM product").query(Long.class).single()).isEqualTo(5);

        var result = flyway("latest").migrate();
        assertThat(result.migrationsExecuted).as("wykonała się tylko brakująca V3").isEqualTo(1);
        assertThat(result.targetSchemaVersion).isEqualTo("3");
        assertThat(productColumns()).last().isEqualTo("weight_grams");
        assertThat(jdbc.sql("SELECT name, weight_grams FROM product ORDER BY id")
                .query((rs, n) -> rs.getString(1) + "=" + rs.getInt(2)).list())
                .containsExactly("Klawiatura=450", "Mysz=90", "Kubek=320", "Lampka=0", "Zeszyt=0");
    }

    @Test
    @DisplayName("Sekcja 4: ponowne migrate na aktualnej bazie niczego nie robi (historia w flyway_schema_history)")
    void migrateIsIdempotent() {
        flyway("latest").migrate();
        assertThat(flyway("latest").migrate().migrationsExecuted).isZero();
        assertThat(flyway("latest").info().pending()).isEmpty();
    }
}
