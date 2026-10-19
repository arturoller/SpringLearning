package s07_data_jdbc.solutions.jdbc04_flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import s07_data_jdbc.jdbc04_flyway.Catalog;
import s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport;
import s07_data_jdbc.jdbc04_flyway.MigrationHistory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jdbc04FlywayExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Jdbc04FlywaySolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: nazwa pliku migracji V4 „create customer”")
    void solution1(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve(Jdbc04FlywaySolutions.solution1()),
                "CREATE TABLE customer (id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL);");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(
                Jdbc04TestSupport.newDatabaseUrl(), migrations, "spring.flyway.validate-migration-naming=true")) {
            assertThat(context.getBean(MigrationHistory.class).describe())
                    .contains("4 | 4 | create customer | SQL | true");
        }
    }

    @Test
    @DisplayName("Wzorzec 2: migracja V4 dodaje kolumnę description")
    void solution2(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve("V4__add_description.sql"), Jdbc04FlywaySolutions.solution2());

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), migrations)) {
            Integer withoutDescription = context.getBean(JdbcClient.class)
                    .sql("SELECT COUNT(*) FROM product WHERE description IS NULL").query(Integer.class).single();
            assertThat(withoutDescription).isEqualTo(6);
        }
    }

    @Test
    @DisplayName("Wzorzec 3: out-of-order pozwala wykonać V3_5 po V4")
    void solution3(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve("V4__add_index.sql"), "CREATE INDEX idx_product_category ON product (category);");
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext ignored = Jdbc04TestSupport.start(url, migrations)) {
            // baza ma już wersje do 4
        }
        Files.writeString(migrations.resolve("V3_5__late.sql"), "CREATE INDEX idx_product_name ON product (name);");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations, Jdbc04FlywaySolutions.solution3())) {
            assertThat(context.getBean(MigrationHistory.class).appliedVersions()).contains("3.5");
        }
    }

    @Test
    @DisplayName("Wzorzec 4: nowa treść migracji powtarzalnej zmienia widok")
    void solution4(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(Catalog.class).skusOfExpensive()).containsExactly("ELE-001");
        }

        Files.writeString(migrations.resolve("R__expensive_products_view.sql"), Jdbc04FlywaySolutions.solution4());

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(Catalog.class).skusOfExpensive()).containsExactly("ELE-001", "ELE-003");
        }
    }
}
