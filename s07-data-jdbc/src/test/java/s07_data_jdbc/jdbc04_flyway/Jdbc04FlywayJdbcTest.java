package s07_data_jdbc.jdbc04_flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc04Flyway na wycinku @JdbcTest (sekcje 1, 4 i 5). Wycinek ma w sobie autokonfigurację Flyway,
 * więc migracje z profilu "jdbc04" wykonują się na wbudowanej bazie podmienionej przez test — test działa na
 * PRAWDZIWYM schemacie z migracji, a nie na jego ręcznej kopii.
 */
@JdbcTest
@ActiveProfiles("jdbc04")
class Jdbc04FlywayJdbcTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource dataSource;

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: historia Flyway zawiera V1, V2, V3 i migrację powtarzalną — wszystkie z sukcesem")
    void section1HistoryTable() {
        List<AppliedMigration> history = Jdbc04MigrationHistory.sqlMigrations(jdbc);

        assertThat(history).extracting(AppliedMigration::installedRank).containsExactly(1, 2, 3, 4);
        assertThat(history).extracting(AppliedMigration::version).containsExactly("1", "2", "3", null);
        assertThat(history).extracting(AppliedMigration::description)
                .as("Flyway zamienia podkreślniki z nazwy pliku na spacje")
                .containsExactly("create product", "add stock", "insert products", "product overview");
        assertThat(history).extracting(AppliedMigration::script)
                .containsExactly("V1__create_product.sql", "V2__add_stock.sql", "V3__insert_products.sql",
                        "R__product_overview.sql");
        assertThat(history).extracting(AppliedMigration::success).containsOnly(true);
        assertThat(history).extracting(AppliedMigration::checksum).doesNotContainNull();
    }

    @Test
    @DisplayName("Sekcja 1: tabela historii ma nazwę z małych liter w cudzysłowie — bez cudzysłowu H2 jej nie znajduje")
    void section1HistoryTableNeedsQuotes() {
        assertThatThrownBy(() -> jdbc.sql("SELECT COUNT(*) FROM flyway_schema_history").query(Integer.class).single())
                .isInstanceOf(BadSqlGrammarException.class)
                .hasStackTraceContaining("FLYWAY_SCHEMA_HISTORY");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM \"flyway_schema_history\"").query(Integer.class).single())
                .isPositive();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: migracja V3 wstawiła pięć produktów, a migracja powtarzalna utworzyła widok")
    void section4DataAndRepeatableView() {
        assertThat(jdbc.sql("SELECT name FROM product ORDER BY id").query(String.class).list())
                .containsExactly("Klawiatura", "Mysz", "Monitor", "Kubek", "Lampka");
        BigDecimal monitorValue = jdbc.sql("SELECT stock_value FROM product_overview WHERE name = 'Monitor'")
                .query(BigDecimal.class).single();
        assertThat(monitorValue).isEqualByComparingTo("4499.95");
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: Flyway zmigrował bazę podmienioną przez @JdbcTest (losowa nazwa), nie bazę z profilu")
    void section5MigratedTheReplacedDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:").doesNotContain("jdbc04");
        }
        assertThat(Jdbc04MigrationHistory.sqlMigrations(jdbc)).hasSize(4);
    }

    @Test
    @DisplayName("Sekcja 5: dane z testu są wycofywane (pierwszy test) — migracje i tak zostają")
    void section5RollbackFirst() {
        jdbc.sql("INSERT INTO product (name, category, price) VALUES ('Zeszyt', 'dom', 4.50)").update();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single()).isEqualTo(6);
    }

    @Test
    @DisplayName("Sekcja 5: dane z testu są wycofywane (drugi test) — znów pięć produktów, niezależnie od kolejności")
    void section5RollbackSecond() {
        jdbc.sql("INSERT INTO product (name, category, price) VALUES ('Zeszyt', 'dom', 4.50)").update();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single()).isEqualTo(6);
    }
}
