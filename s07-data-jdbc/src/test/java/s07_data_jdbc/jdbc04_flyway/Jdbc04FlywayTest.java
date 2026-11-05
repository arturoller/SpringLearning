package s07_data_jdbc.jdbc04_flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.CLASSPATH_MIGRATIONS;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.PROFILE;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.flywayOn;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.jdbcOn;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.memUrl;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.newDataSource;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.start;
import static s07_data_jdbc.jdbc04_flyway.Jdbc04TestSupport.write;

/**
 * Testy lekcji Jdbc04Flyway (sekcje 1–5, część bez @JdbcTest). To zwykłe testy JUnit, które same uruchamiają
 * Flyway (programowo, na plikach z katalogu tymczasowego) albo całą aplikację lekcji z różnymi ustawieniami —
 * tak da się pokazać błędy, których nie wolno mieć w prawdziwym projekcie. Historię i dane z migracji lekcji
 * sprawdza Jdbc04FlywayJdbcTest.
 */
class Jdbc04FlywayTest {

    @TempDir
    Path directory;

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: plik z jednym podkreślnikiem (V2_zle.sql) nie jest migracją — Flyway go po cichu pomija")
    void section1SingleUnderscoreIsIgnored() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        write(directory, "V2_zle.sql", "CREATE TABLE t2 (id INT);");
        DataSource dataSource = newDataSource();

        Flyway flyway = flywayOn(dataSource, directory);
        assertThat(flyway.info().all()).extracting(MigrationInfo::getScript).containsExactly("V1__create_t.sql");
        flyway.migrate();

        assertThatThrownBy(() -> jdbcOn(dataSource).sql("SELECT COUNT(*) FROM t2").query(Integer.class).single())
                .isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    @DisplayName("Sekcja 1: validateMigrationNaming(true) wykrywa źle nazwany plik i podaje jego nazwę")
    void section1NamingValidation() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        write(directory, "V2_zle.sql", "CREATE TABLE t2 (id INT);");

        Flyway strict = Flyway.configure().dataSource(newDataSource()).locations("filesystem:" + directory)
                .validateMigrationNaming(true).load();

        assertThatThrownBy(strict::validate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Invalid SQL filenames found")
                .hasMessageContaining("V2_zle.sql");
    }

    @Test
    @DisplayName("Sekcja 1: migracje wykonują się w kolejności numerów (V2 przed V10), a nie alfabetycznie")
    void section1VersionOrderIsNumeric() {
        write(directory, "V10__third.sql", "INSERT INTO log (msg) VALUES ('dziesiata');");
        write(directory, "V2__second.sql", "INSERT INTO log (msg) VALUES ('druga');");
        write(directory, "V1__first.sql", "CREATE TABLE log (id INT AUTO_INCREMENT PRIMARY KEY, msg VARCHAR(20));");
        DataSource dataSource = newDataSource();

        flywayOn(dataSource, directory).migrate();

        assertThat(jdbcOn(dataSource).sql("SELECT msg FROM log ORDER BY id").query(String.class).list())
                .containsExactly("druga", "dziesiata");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: Boot uruchamia Flyway przed beanami zależnymi od bazy — bean już widzi tabelę i dane")
    void section2FlywayRunsBeforeOtherBeans() {
        try (ConfigurableApplicationContext app = start(PROFILE)) {
            assertThat(app.getBean(Jdbc04StartupReport.class).productsAtStartup())
                    .as("w konstruktorze beana tabela product miała już 5 wierszy z migracji V3")
                    .isEqualTo(5);
            Flyway flyway = app.getBean(Flyway.class);
            MigrationInfo[] applied = flyway.info().applied();
            assertThat(applied).hasSize(4).extracting(MigrationInfo::getState)
                    .containsOnly(MigrationState.SUCCESS);
            assertThat(flyway.info().pending()).isEmpty();
        }
    }

    @Test
    @DisplayName("Sekcja 2: bez profilu jdbc04 (spring.flyway.enabled=false ze wspólnego pliku) nie ma beana Flyway")
    void section2DisabledByDefaultInThisModule() {
        try (ConfigurableApplicationContext app = start("spring.datasource.url=" + memUrl())) {
            assertThat(app.getBeanNamesForType(Flyway.class)).isEmpty();
        }
    }

    @Test
    @DisplayName("Sekcja 2: Flyway używa tego samego DataSource co aplikacja i domyślnie ma wyłączone czyszczenie bazy")
    void section2CleanDisabledByDefault() {
        try (ConfigurableApplicationContext app = start(PROFILE)) {
            Flyway flyway = app.getBean(Flyway.class);
            assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
            assertThatThrownBy(flyway::clean)
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("Unable to execute clean as it has been disabled");
            assertThat(app.getBean(JdbcClient.class).sql("SELECT COUNT(*) FROM product")
                    .query(Integer.class).single()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("Sekcja 2: literówka w spring.flyway.locations nie zatrzymuje startu (fail-on-missing-locations=false)")
    void section2MissingLocationIsSilent() {
        try (ConfigurableApplicationContext app = start(PROFILE,
                "spring.flyway.locations=classpath:db/migration/nie-ma-takiego", "jdbc04.report=false")) {
            assertThat(app.getBean(Flyway.class).info().all()).isEmpty();
            assertThatThrownBy(() -> app.getBean(JdbcClient.class).sql("SELECT COUNT(*) FROM product")
                    .query(Integer.class).single())
                    .as("brak migracji = brak tabeli, błąd dopiero przy pierwszym zapytaniu")
                    .isInstanceOf(BadSqlGrammarException.class);
        }
    }

    @Test
    @DisplayName("Sekcja 2: spring.flyway.fail-on-missing-locations=true zamienia tę literówkę w błąd przy starcie")
    void section2FailOnMissingLocations() {
        assertThatThrownBy(() -> start(PROFILE,
                "spring.flyway.locations=classpath:db/migration/nie-ma-takiego",
                "spring.flyway.fail-on-missing-locations=true"))
                .isInstanceOf(BeanCreationException.class)
                .hasStackTraceContaining("Failed to initialize dependency 'flywayInitializer' of JdbcClient bean")
                .hasStackTraceContaining("Unable to resolve location classpath:db/migration/nie-ma-takiego");
    }

    @Test
    @DisplayName("Sekcja 2: baza z tabelami, ale bez historii Flyway — migrate() odmawia, baseline-on-migrate ją przyjmuje")
    void section2BaselineOnMigrate() {
        write(directory, "V1__create_old.sql", "CREATE TABLE old_table (id INT);");
        write(directory, "V2__add_new.sql", "CREATE TABLE new_table (id INT);");
        DataSource existingDatabase = newDataSource();
        jdbcOn(existingDatabase).sql("CREATE TABLE old_table (id INT)").update(); // tabela sprzed Flyway

        assertThatThrownBy(() -> flywayOn(existingDatabase, directory).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Found non-empty schema(s)")
                .hasMessageContaining("baselineOnMigrate");

        Flyway withBaseline = Flyway.configure().dataSource(existingDatabase)
                .locations("filesystem:" + directory)
                .baselineOnMigrate(true).baselineVersion("1").load();
        withBaseline.migrate();

        assertThat(Arrays.stream(withBaseline.info().all()).map(i -> i.getVersion() + ":" + i.getState()).toList())
                .as("plik V1 pominięty (BASELINE_IGNORED), bo baza ma już stan wersji 1 (BASELINE); wykonała się tylko V2")
                .containsExactlyInAnyOrder("1:BASELINE_IGNORED", "1:BASELINE", "2:SUCCESS");
        assertThat(jdbcOn(existingDatabase).sql("SELECT COUNT(*) FROM new_table").query(Integer.class).single())
                .isZero();
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: zmiana pliku migracji, który już poszedł na bazę, kończy się FlywayValidateException (checksum)")
    void section3ChecksumMismatch() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        DataSource dataSource = newDataSource();
        flywayOn(dataSource, directory).migrate();

        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT, extra INT);"); // ktoś „poprawił” stary plik

        Flyway again = flywayOn(dataSource, directory);
        assertThatThrownBy(again::validate)
                .isInstanceOf(FlywayValidateException.class)
                .hasMessageContaining("Migration checksum mismatch for migration version 1");
        assertThatThrownBy(again::migrate)
                .as("migrate() też najpierw waliduje (validate-on-migrate=true) i niczego nie wykona")
                .isInstanceOf(FlywayValidateException.class);
    }

    @Test
    @DisplayName("Sekcja 3: błąd checksum zatrzymuje start całej aplikacji Spring Boot")
    void section3ChecksumMismatchStopsApplication() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        List<String> settings = List.of("spring.datasource.url=" + memUrl(), "spring.flyway.enabled=true",
                "spring.flyway.locations=filesystem:" + directory);

        try (ConfigurableApplicationContext first = start(settings)) {
            assertThat(first.getBean(Flyway.class).info().applied()).hasSize(1);
        }
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT, extra INT);");

        assertThatThrownBy(() -> start(settings)) // ta sama baza (DB_CLOSE_DELAY=-1) i ten sam katalog, plik już zmieniony
                .isInstanceOf(BeanCreationException.class)
                .hasStackTraceContaining("flywayInitializer")
                .hasStackTraceContaining("Migration checksum mismatch for migration version 1");
    }

    @Test
    @DisplayName("Sekcja 3: poprawna zmiana to NOWA migracja V2 — stary plik zostaje nietknięty, walidacja przechodzi")
    void section3NewMigrationInsteadOfEdit() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        DataSource dataSource = newDataSource();
        flywayOn(dataSource, directory).migrate();

        write(directory, "V2__add_extra.sql", "ALTER TABLE t ADD COLUMN extra INT;");
        Flyway again = flywayOn(dataSource, directory);
        assertThat(again.migrate().migrationsExecuted).isEqualTo(1);

        assertThat(jdbcOn(dataSource).sql("SELECT COUNT(*) FROM t WHERE extra IS NULL").query(Integer.class).single())
                .isZero();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: migracja powtarzalna R__ wykonuje się ponownie dopiero po zmianie treści")
    void section4RepeatableMigration() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT, big INT);");
        write(directory, "R__view.sql", "CREATE OR REPLACE VIEW v AS SELECT id FROM t;");
        DataSource dataSource = newDataSource();

        assertThat(flywayOn(dataSource, directory).migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flywayOn(dataSource, directory).migrate().migrationsExecuted)
                .as("nic się nie zmieniło — nic do wykonania").isZero();

        write(directory, "R__view.sql", "CREATE OR REPLACE VIEW v AS SELECT id, big FROM t;");
        assertThat(flywayOn(dataSource, directory).migrate().migrationsExecuted)
                .as("zmieniła się treść R__ — wykona się ponownie (a V1 nie)").isEqualTo(1);

        List<AppliedMigration> history = Jdbc04MigrationHistory.sqlMigrations(jdbcOn(dataSource));
        assertThat(history).extracting(AppliedMigration::script)
                .containsExactly("V1__create_t.sql", "R__view.sql", "R__view.sql");
        assertThat(history).extracting(AppliedMigration::version).containsExactly("1", null, null);
    }

    @Test
    @DisplayName("Sekcja 4: migracja powtarzalna wykonuje się po wersjonowanych, nawet gdy ma „wcześniejszą” nazwę")
    void section4RepeatableRunsAfterVersioned() {
        write(directory, "R__a_view.sql", "CREATE OR REPLACE VIEW v AS SELECT id FROM t;");
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        DataSource dataSource = newDataSource();

        flywayOn(dataSource, directory).migrate(); // gdyby R__ poszło pierwsze, widok nie miałby tabeli t

        assertThat(Jdbc04MigrationHistory.sqlMigrations(jdbcOn(dataSource)))
                .extracting(AppliedMigration::script).containsExactly("V1__create_t.sql", "R__a_view.sql");
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: clean kasuje całą bazę — działa tylko po świadomym wyłączeniu blokady (cleanDisabled=false)")
    void section5CleanWipesTheDatabase() {
        write(directory, "V1__create_t.sql", "CREATE TABLE t (id INT);");
        DataSource dataSource = newDataSource();
        flywayOn(dataSource, directory).migrate();

        assertThatThrownBy(() -> flywayOn(dataSource, directory).clean()).isInstanceOf(FlywayException.class);

        Flyway unlocked = Flyway.configure().dataSource(dataSource).locations("filesystem:" + directory)
                .cleanDisabled(false).load();
        unlocked.clean();

        assertThatThrownBy(() -> jdbcOn(dataSource).sql("SELECT COUNT(*) FROM t").query(Integer.class).single())
                .isInstanceOf(BadSqlGrammarException.class);
        assertThat(unlocked.info().pending()).hasSize(1); // Flyway znów widzi V1 jako do wykonania
    }

    @Test
    @DisplayName("Sekcja 5: lokalizacja lekcji (classpath:db/migration/jdbc04) zawiera dokładnie cztery migracje")
    void section5LessonLocationContents() {
        Flyway flyway = Flyway.configure().dataSource(newDataSource()).locations(CLASSPATH_MIGRATIONS).load();
        assertThat(flyway.info().all()).extracting(MigrationInfo::getScript)
                .containsExactly("V1__create_product.sql", "V2__add_stock.sql", "V3__insert_products.sql",
                        "R__product_overview.sql");
    }
}
