package s07_data_jdbc.solutions.jdbc04_flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import s07_data_jdbc.jdbc04_flyway.AppliedMigration;
import s07_data_jdbc.jdbc04_flyway.Jdbc04MigrationHistory;

import javax.sql.DataSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.PROFILE;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.flywayOnLessonAnd;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.jdbcOn;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.newDataSource;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.start;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.write;

/** Sprawdzenia z Jdbc04FlywayExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Jdbc04FlywaySolutionsTest {

    @TempDir
    Path directory;

    @Test
    @DisplayName("Wzorzec 1: nazwa pliku migracji z dwoma podkreślnikami i opisem z podkreślnikami")
    void solution1() {
        assertThat(Jdbc04FlywaySolutions.solution1MigrationFileName(5, "Add discount percent"))
                .isEqualTo("V5__add_discount_percent.sql");
        assertThat(Jdbc04FlywaySolutions.solution1MigrationFileName(10, "create orders")).isEqualTo("V10__create_orders.sql");
    }

    @Test
    @DisplayName("Wzorzec 2: ostatnia wykonana wersja z tabeli historii (także gdy wersji jest dziesięć)")
    void solution2() {
        try (ConfigurableApplicationContext app = start(PROFILE)) {
            assertThat(Jdbc04FlywaySolutions.solution2LastVersion(app.getBean(JdbcClient.class))).isEqualTo("3");
        }
        DataSource dataSource = newDataSource();
        for (int version = 4; version <= 10; version++) {
            write(directory, "V" + version + "__noop_" + version + ".sql", "SELECT 1;");
        }
        flywayOnLessonAnd(dataSource, directory).migrate();
        assertThat(Jdbc04FlywaySolutions.solution2LastVersion(jdbcOn(dataSource))).isEqualTo("10");
    }

    @Test
    @DisplayName("Wzorzec 3: migracja V4 dodaje kolumnę discount_percent, która dopuszcza NULL")
    void solution3() {
        write(directory, "V4__add_discount_percent.sql", Jdbc04FlywaySolutions.solution3AddDiscountSql());
        DataSource dataSource = newDataSource();

        flywayOnLessonAnd(dataSource, directory).migrate();

        assertThat(jdbcOn(dataSource).sql("SELECT COUNT(*) FROM product WHERE discount_percent IS NULL")
                .query(Integer.class).single()).isEqualTo(5);
        assertThat(Jdbc04MigrationHistory.sqlMigrations(jdbcOn(dataSource)))
                .extracting(AppliedMigration::version).contains("4");
    }

    @Test
    @DisplayName("Wzorzec 4: migracja powtarzalna tworzy widok, a drugie uruchomienie nic nie wykonuje")
    void solution4() {
        write(directory, "R__expensive_product.sql", Jdbc04FlywaySolutions.solution4ExpensiveProductViewSql());
        DataSource dataSource = newDataSource();

        flywayOnLessonAnd(dataSource, directory).migrate();
        assertThat(flywayOnLessonAnd(dataSource, directory).migrate().migrationsExecuted).isZero();

        assertThat(jdbcOn(dataSource).sql("SELECT name FROM expensive_product ORDER BY name")
                .query(String.class).list()).containsExactly("Klawiatura", "Monitor");
    }
}
