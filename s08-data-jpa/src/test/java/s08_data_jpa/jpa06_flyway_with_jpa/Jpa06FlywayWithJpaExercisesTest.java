package s08_data_jpa.jpa06_flyway_with_jpa;

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

/** Testy ĆWICZEŃ lekcji Jpa06FlywayWithJpa (metody exerciseN w klasie lekcji). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jpa06FlywayWithJpaExercisesTest {

    private final DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:jpa06-exercise-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
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
    @DisplayName("Ćwiczenie 1: nazwa pliku migracji Flyway")
    void exercise1() {
        assertThat(Jpa06FlywayWithJpa.exercise1MigrationFileName(4, "add supplier column")).isEqualTo("V4__add_supplier_column.sql");
        assertThat(Jpa06FlywayWithJpa.exercise1MigrationFileName(12, "create order")).isEqualTo("V12__create_order.sql");
    }

    @Test
    @DisplayName("Ćwiczenie 2: kolumny tabeli w kolejności z bazy")
    void exercise2() {
        migrate("latest");
        assertThat(Jpa06FlywayWithJpa.exercise2Columns(dataSource, "PRODUCT"))
                .containsExactly("id", "name", "category", "price", "stock", "created_at", "weight_grams");
    }

    @Test
    @DisplayName("Ćwiczenie 3: migracja V4 — kolumna supplier (może być pusta) i dane dla COMPUTERS")
    void exercise3() {
        migrate("latest");
        Jpa06FlywayWithJpa.exercise3SupplierMigration().forEach(sql -> jdbc.sql(sql).update());

        assertThat(jdbc.sql("SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_name = 'PRODUCT' AND column_name = 'SUPPLIER'").query(String.class).single()).isEqualTo("YES");
        assertThat(jdbc.sql("SELECT name || '=' || COALESCE(supplier, '-') FROM product ORDER BY id").query(String.class).list())
                .containsExactly("Klawiatura=Nowak", "Mysz=Nowak", "Kubek=-", "Lampka=-", "Zeszyt=-");
    }

    @Test
    @DisplayName("Ćwiczenie 4: wersje migracji, które jeszcze czekają na wykonanie")
    void exercise4() {
        migrate("2");
        assertThat(Jpa06FlywayWithJpa.exercise4PendingVersions(dataSource)).containsExactly("3");

        migrate("latest");
        assertThat(Jpa06FlywayWithJpa.exercise4PendingVersions(dataSource)).isEmpty();
    }
}
