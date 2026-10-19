package s07_data_jdbc.jdbc04_flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc04Flyway. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Jdbc04FlywayExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: nazwa pliku migracji V4 „create customer”")
    void exercise1(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve(Jdbc04Flyway.exercise1()),
                "CREATE TABLE customer (id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL);");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(
                Jdbc04TestSupport.newDatabaseUrl(), migrations, "spring.flyway.validate-migration-naming=true")) {
            assertThat(context.getBean(MigrationHistory.class).describe())
                    .contains("4 | 4 | create customer | SQL | true");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: migracja V4 dodaje kolumnę description")
    void exercise2(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve("V4__add_description.sql"), Jdbc04Flyway.exercise2());

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), migrations)) {
            Integer withoutDescription = context.getBean(JdbcClient.class)
                    .sql("SELECT COUNT(*) FROM product WHERE description IS NULL").query(Integer.class).single();
            assertThat(withoutDescription).isEqualTo(6);   // kolumna istnieje, istniejące produkty mają NULL
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: out-of-order pozwala wykonać V3_5 po V4")
    void exercise3(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve("V4__add_index.sql"), "CREATE INDEX idx_product_category ON product (category);");
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext ignored = Jdbc04TestSupport.start(url, migrations)) {
            // baza ma już wersje do 4
        }
        Files.writeString(migrations.resolve("V3_5__late.sql"), "CREATE INDEX idx_product_name ON product (name);");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations, Jdbc04Flyway.exercise3())) {
            assertThat(context.getBean(MigrationHistory.class).appliedVersions()).contains("3.5");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: nowa treść migracji powtarzalnej zmienia widok")
    void exercise4(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(Catalog.class).skusOfExpensive()).containsExactly("ELE-001");
        }

        Files.writeString(migrations.resolve("R__expensive_products_view.sql"), Jdbc04Flyway.exercise4());

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(Catalog.class).skusOfExpensive()).containsExactly("ELE-001", "ELE-003");
        }
    }
}
