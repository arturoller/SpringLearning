package s07_data_jdbc.jdbc04_flyway;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc04Flyway — „wynik” każdej sekcji.
 *
 * <p>Zwykły {@code @SpringBootTest} tu nie wystarcza: scenariusze wymagają KILKU kolejnych startów aplikacji na tej samej
 * bazie (jak kolejne wdrożenia) i zmian w plikach migracji między startami. Test kopiuje więc migracje lekcji do katalogu
 * tymczasowego ({@code @TempDir}), a aplikację uruchamia przez Jdbc04TestSupport. Baza H2 ma DB_CLOSE_DELAY=-1, więc
 * przeżywa zamknięcie kontekstu — tak jak trwała baza przeżywa restart aplikacji.</p>
 */
class Jdbc04FlywayTest {

    @Test
    @DisplayName("1. Przy starcie Flyway wykonuje V1, V2, V3 i migrację powtarzalną; historia je zapisuje")
    void section1() {
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), null)) {
            Catalog catalog = context.getBean(Catalog.class);
            MigrationHistory history = context.getBean(MigrationHistory.class);

            assertThat(catalog.count()).isEqualTo(6);                         // V1 + V2
            assertThat(catalog.stockOf("ELE-001")).isEqualTo(7);              // V3
            assertThat(catalog.skusOfExpensive()).containsExactly("ELE-001"); // R__ (cena >= 1000)

            assertThat(history.appliedVersions()).containsExactly("1", "2", "3");
            assertThat(history.currentVersion()).isEqualTo("3");
            assertThat(history.describe()).contains(
                    "1 | 1 | create product | SQL | true",
                    "2 | 2 | insert products | SQL | true",
                    "3 | 3 | add stock column | SQL | true",
                    "4 | null | expensive products view | SQL | true");   // powtarzalna: wersja NULL
        }
    }

    @Test
    @DisplayName("2. Kolejny start wykonuje tylko NOWE migracje")
    void section2(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        String url = Jdbc04TestSupport.newDatabaseUrl();

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(MigrationHistory.class).describe()).hasSize(5);   // wpis o tabeli historii + 4 migracje
        }
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(MigrationHistory.class).describe()).hasSize(5);   // drugi start: nic nowego
        }

        Files.writeString(migrations.resolve("V4__add_index.sql"), "CREATE INDEX idx_product_category ON product (category);");
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            MigrationHistory history = context.getBean(MigrationHistory.class);
            assertThat(history.appliedVersions()).containsExactly("1", "2", "3", "4");
            assertThat(history.describe()).hasSize(6).contains("5 | 4 | add index | SQL | true");   // tylko V4 dopisana
        }
    }

    @Test
    @DisplayName("3. Zmiana już wykonanej migracji kończy się błędem sumy kontrolnej")
    void section3(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext ignored = Jdbc04TestSupport.start(url, migrations)) {
            // pierwszy start: migracje wykonane, sumy kontrolne zapisane w historii
        }

        Path v1 = migrations.resolve("V1__create_product.sql");
        Files.writeString(v1, Files.readString(v1) + "\n-- niewinny komentarz dopisany po fakcie\n");

        assertThatThrownBy(() -> Jdbc04TestSupport.start(url, migrations))
                .rootCause()
                .isInstanceOf(FlywayValidateException.class)
                .hasMessageContaining("Migration checksum mismatch for migration version 1");
    }


    @Test
    @DisplayName("3b. Końce linii (LF/CRLF) nie zmieniają sumy kontrolnej, zmiana treści linii — tak")
    void section3bChecksumAndLineEndings(@TempDir Path lf, @TempDir Path crlf, @TempDir Path spaced) throws IOException {
        String sql = "CREATE TABLE a (\n    id INT\n);\n";
        Files.writeString(lf.resolve("V1__create_a.sql"), sql);
        Files.writeString(crlf.resolve("V1__create_a.sql"), sql.replace("\n", "\r\n"));
        Files.writeString(spaced.resolve("V1__create_a.sql"), sql.replace("id INT", "id  INT"));   // dodatkowa spacja

        Integer checksumLf = checksumOf(lf);
        assertThat(checksumOf(crlf)).isEqualTo(checksumLf);
        assertThat(checksumOf(spaced)).isNotEqualTo(checksumLf);
    }

    private static Integer checksumOf(Path directory) {
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource("jdbc:h2:mem:jdbc04_crc_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "")
                .locations("filesystem:" + directory.toString().replace('\\', '/'))
                .load();
        flyway.migrate();
        return java.util.Arrays.stream(flyway.info().applied())
                .filter(info -> info.getVersion() != null)
                .findFirst().orElseThrow().getChecksum();
    }
    @Test
    @DisplayName("4. Błędne nazwy plików są pomijane w ciszy, a wersje porównuje się liczbowo")
    void section4(@TempDir Path migrations) throws IOException {
        Files.writeString(migrations.resolve("V1_create.sql"), "CREATE TABLE a (id INT);");         // jedno podkreślenie
        Files.writeString(migrations.resolve("v2__lower.sql"), "CREATE TABLE b (id INT);");         // mała litera
        Files.writeString(migrations.resolve("V3__ok.sql"), "CREATE TABLE c (id INT);");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), migrations)) {
            assertThat(context.getBean(MigrationHistory.class).appliedVersions()).containsExactly("3");   // tylko poprawny
        }
        assertThatThrownBy(() -> Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), migrations,
                "spring.flyway.validate-migration-naming=true"))
                .hasStackTraceContaining("Invalid SQL filenames found")
                .hasStackTraceContaining("Invalid versioned migration name format: V1_create.sql");

        // nieistniejący katalog: domyślnie bez błędu i bez migracji
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), null,
                "spring.flyway.locations=classpath:db/migration/nie_ma_takiego_katalogu")) {
            assertThat(context.getBean(MigrationHistory.class).appliedVersions()).isEmpty();
        }
    }

    @Test
    @DisplayName("4b. Wersje V2, V9, V10 wykonują się w kolejności liczbowej, nie alfabetycznej")
    void section4NumericOrder(@TempDir Path migrations) throws IOException {
        Files.writeString(migrations.resolve("V10__c.sql"), "CREATE TABLE c (id INT);");
        Files.writeString(migrations.resolve("V2__a.sql"), "CREATE TABLE a (id INT);");
        Files.writeString(migrations.resolve("V9__b.sql"), "CREATE TABLE b (id INT);");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), migrations)) {
            assertThat(context.getBean(MigrationHistory.class).appliedVersions()).containsExactly("2", "9", "10");
        }
    }

    @Test
    @DisplayName("5. Migracja powtarzalna wykonuje się ponownie po zmianie treści")
    void section5(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(Catalog.class).skusOfExpensive()).containsExactly("ELE-001");
        }

        Path view = migrations.resolve("R__expensive_products_view.sql");
        Files.writeString(view, Files.readString(view).replace("1000", "100"));   // próg 100 zł

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations)) {
            assertThat(context.getBean(Catalog.class).skusOfExpensive())
                    .containsExactly("DOM-002", "ELE-001", "ELE-003", "KSI-002");   // ceny >= 100, wg SKU
            assertThat(context.getBean(MigrationHistory.class).describe())
                    .contains("5 | null | expensive products view | SQL | true");     // dopisana ponownie
        }
    }

    @Test
    @DisplayName("6. Migracja o numerze niższym niż wykonane: błąd walidacji albo zgoda przez out-of-order")
    void section6(@TempDir Path migrations) throws IOException {
        Jdbc04TestSupport.copyLessonMigrations(migrations);
        Files.writeString(migrations.resolve("V4__add_index.sql"), "CREATE INDEX idx_product_category ON product (category);");
        String url = Jdbc04TestSupport.newDatabaseUrl();
        try (ConfigurableApplicationContext ignored = Jdbc04TestSupport.start(url, migrations)) {
            // baza ma już wersje do 4
        }

        Files.writeString(migrations.resolve("V3_5__late.sql"), "CREATE INDEX idx_product_name ON product (name);");

        assertThatThrownBy(() -> Jdbc04TestSupport.start(url, migrations))
                .rootCause()
                .isInstanceOf(FlywayValidateException.class)
                .hasMessageContaining("Detected resolved migration not applied to database: 3.5");

        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(url, migrations,
                "spring.flyway.out-of-order=true")) {
            assertThat(context.getBean(MigrationHistory.class).appliedVersions()).contains("3.5");
        }
    }

    @Test
    @DisplayName("7. clean jest domyślnie wyłączone i kończy się wyjątkiem")
    void section7() {
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), null)) {
            MigrationHistory history = context.getBean(MigrationHistory.class);
            assertThatThrownBy(() -> history.flyway().clean())
                    .isInstanceOf(FlywayException.class)
                    .hasMessage("Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.");
        }
        try (ConfigurableApplicationContext context = Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), null,
                "spring.flyway.clean-disabled=false")) {
            MigrationHistory history = context.getBean(MigrationHistory.class);
            history.flyway().clean();                                           // świadomie włączone: czyści wszystko
            assertThat(history.flyway().info().applied()).isEmpty();
        }
    }

    @Test
    @DisplayName("8. Błąd SQL w migracji zatrzymuje start aplikacji")
    void section8(@TempDir Path migrations) throws IOException {
        Files.writeString(migrations.resolve("V1__ok.sql"), "CREATE TABLE a (id INT);");
        Files.writeString(migrations.resolve("V2__bad.sql"), "INSERT INTO tabela_ktorej_nie_ma VALUES (1);");

        assertThatThrownBy(() -> Jdbc04TestSupport.start(Jdbc04TestSupport.newDatabaseUrl(), migrations))
                .hasStackTraceContaining("FlywayMigrateException")
                .hasStackTraceContaining("Failed to execute script V2__bad.sql");
    }
}
