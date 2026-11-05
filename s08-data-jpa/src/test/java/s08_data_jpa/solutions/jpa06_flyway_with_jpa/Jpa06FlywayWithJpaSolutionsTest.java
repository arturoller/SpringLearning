package s08_data_jpa.solutions.jpa06_flyway_with_jpa;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jpa06FlywayWithJpaExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Jpa06FlywayWithJpaSolutionsTest {

    private final DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:jpa06-solution-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    private final JdbcClient jdbc = JdbcClient.create(dataSource);

    @AfterEach
    void dropDatabase() {
        jdbc.sql("SHUTDOWN").update();
    }

    /** Migracje lekcji do wersji {@code target} ("latest" = wszystkie). */
    private void migrate(String target) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/jpa06").target(target).load().migrate();
    }

    @Test
    @DisplayName("Wzorzec 1: nazwa pliku migracji Flyway")
    void solution1() {
        assertThat(Jpa06FlywayWithJpaSolutions.solution1MigrationFileName(4, "add supplier column")).isEqualTo("V4__add_supplier_column.sql");
        assertThat(Jpa06FlywayWithJpaSolutions.solution1MigrationFileName(12, "create order")).isEqualTo("V12__create_order.sql");
    }

    @Test
    @DisplayName("Wzorzec 2: kolumny tabeli w kolejności z bazy")
    void solution2() {
        migrate("latest");
        assertThat(Jpa06FlywayWithJpaSolutions.solution2Columns(dataSource, "PRODUCT"))
                .containsExactly("id", "name", "category", "price", "stock", "created_at", "weight_grams");
    }

    @Test
    @DisplayName("Wzorzec 3: migracja V4 — kolumna supplier (może być pusta) i dane dla COMPUTERS")
    void solution3() {
        migrate("latest");
        Jpa06FlywayWithJpaSolutions.solution3SupplierMigration().forEach(sql -> jdbc.sql(sql).update());

        assertThat(jdbc.sql("SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_name = 'PRODUCT' AND column_name = 'SUPPLIER'").query(String.class).single()).isEqualTo("YES");
        assertThat(jdbc.sql("SELECT name || '=' || COALESCE(supplier, '-') FROM product ORDER BY id").query(String.class).list())
                .containsExactly("Klawiatura=Nowak", "Mysz=Nowak", "Kubek=-", "Lampka=-", "Zeszyt=-");
    }

    @Test
    @DisplayName("Wzorzec 4: wersje migracji, które jeszcze czekają na wykonanie")
    void solution4() {
        migrate("2");
        assertThat(Jpa06FlywayWithJpaSolutions.solution4PendingVersions(dataSource)).containsExactly("3");

        migrate("latest");
        assertThat(Jpa06FlywayWithJpaSolutions.solution4PendingVersions(dataSource)).isEmpty();
    }
}
