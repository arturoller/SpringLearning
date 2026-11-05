package s07_data_jdbc.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.PROFILE;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.countProducts;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.fileUrl;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.memUrl;
import static s07_data_jdbc.jdbc03_sql_init.Jdbc03TestApps.start;

/**
 * Testy lekcji Jdbc03SqlInit (sekcje 1, 2 i 4). To zwykłe testy JUnit, które same uruchamiają aplikację lekcji
 * z różnymi ustawieniami — wycinek @JdbcTest ma jedno ustawienie na cały test, a tu porównujemy wiele wariantów.
 * Sekcję 3 (@Sql) sprawdza Jdbc03SqlAnnotationTest.
 */
class Jdbc03SqlInitTest {

    @TempDir
    Path tempDirectory;

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: profil jdbc03 tworzy tabelę ze schema.sql i wstawia 3 produkty z data.sql")
    void section1ScriptsRun() {
        try (ConfigurableApplicationContext app = start(PROFILE, memUrl())) {
            assertThat(countProducts(app)).isEqualTo(3);
            assertThat(app.getBean(JdbcTemplate.class)
                    .queryForList("SELECT name FROM product ORDER BY id", String.class))
                    .containsExactly("Mysz", "Kubek", "Lampka");
        }
    }

    @Test
    @DisplayName("Sekcja 1: bez skryptów (brak profilu) nie ma tabeli, a mode=never wyłącza skrypty nawet przy ich podaniu")
    void section1NoScriptsAndNever() {
        try (ConfigurableApplicationContext app = start(memUrl())) {
            assertThatThrownBy(() -> countProducts(app))
                    .isInstanceOf(BadSqlGrammarException.class)
                    .hasStackTraceContaining("Table \"PRODUCT\" not found");
        }
        try (ConfigurableApplicationContext app = start(PROFILE, memUrl(), "spring.sql.init.mode=never")) {
            assertThatThrownBy(() -> countProducts(app)).isInstanceOf(BadSqlGrammarException.class);
        }
    }

    @Test
    @DisplayName("Sekcja 1: platforma dokłada schema-${platform}.sql i data-${platform}.sql z korzenia classpath")
    void section1Platform() {
        try (ConfigurableApplicationContext app = start(memUrl(), "spring.sql.init.platform=jdbc03platform")) {
            assertThat(app.getBean(JdbcTemplate.class).queryForList("SELECT name FROM product", String.class))
                    .containsExactly("Platformowy produkt");
        }
        try (ConfigurableApplicationContext app = start(memUrl())) { // bez platformy tych plików nikt nie czyta
            assertThatThrownBy(() -> countProducts(app)).isInstanceOf(BadSqlGrammarException.class);
        }
    }

    @Test
    @DisplayName("Sekcja 1: znacznik @@platform@@ w jawnej lokalizacji nie jest podstawiany — Boot nie znajduje pliku")
    void section1PlatformPlaceholderInExplicitLocation() {
        assertThatThrownBy(() -> start(memUrl(),
                "spring.sql.init.schema-locations=classpath:jdbc03/schema-@@platform@@.sql",
                "spring.sql.init.platform=h2"))
                .isInstanceOf(BeanCreationException.class)
                .hasStackTraceContaining("No schema scripts found at location");
    }

    @Test
    @DisplayName("Sekcja 1: lokalizacja z prefiksem optional: jest pomijana, gdy pliku nie ma")
    void section1OptionalLocation() {
        try (ConfigurableApplicationContext app = start(PROFILE, memUrl(),
                "spring.sql.init.data-locations=optional:classpath:jdbc03/nie-ma-takiego-pliku.sql")) {
            assertThat(countProducts(app)).as("tabela jest, danych nikt nie wstawił").isZero();
        }
    }

    @Test
    @DisplayName("Sekcja 1: spring.sql.init.enabled=false NIE wyłącza skryptów w Boot 4.1.1 (robi to mode=never)")
    void section1EnabledPropertyDoesNotSwitchScriptsOff() {
        try (ConfigurableApplicationContext app = start(PROFILE, memUrl(), "spring.sql.init.enabled=false")) {
            assertThat(countProducts(app)).isEqualTo(3);
        }
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: same data-locations bez schematu — start kończy się błędem, bo tabeli jeszcze nie ma")
    void section2SchemaBeforeData() {
        assertThatThrownBy(() -> start(PROFILE, memUrl(), "spring.sql.init.schema-locations="))
                .isInstanceOf(BeanCreationException.class)
                .hasCauseInstanceOf(ScriptStatementFailedException.class)
                .hasStackTraceContaining("PRODUCT");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4a: mode=embedded na bazie plikowej NIE uruchamia skryptów, mode=always tak")
    void section4aEmbeddedModeSkipsNonEmbeddedDatabase() {
        Path directory = tempDirectory.resolve("b");
        try (ConfigurableApplicationContext app = start(PROFILE, fileUrl(directory), "spring.sql.init.mode=embedded")) {
            assertThatThrownBy(() -> countProducts(app)).isInstanceOf(BadSqlGrammarException.class);
        }
        try (ConfigurableApplicationContext app = start(PROFILE, fileUrl(tempDirectory.resolve("c")), "spring.sql.init.mode=always")) {
            assertThat(countProducts(app)).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Sekcja 4b: drugi start na trwałej bazie wywala się na CREATE TABLE, a wariant IF NOT EXISTS przechodzi")
    void section4bSecondStartOnPersistentDatabase() {
        Path unsafe = tempDirectory.resolve("unsafe");
        try (ConfigurableApplicationContext first = start(PROFILE, fileUrl(unsafe))) {
            assertThat(countProducts(first)).isEqualTo(3);
        }
        assertThatThrownBy(() -> start(PROFILE, fileUrl(unsafe)))
                .isInstanceOf(BeanCreationException.class)
                .hasCauseInstanceOf(ScriptStatementFailedException.class)
                .hasStackTraceContaining("already exists");

        Path safe = tempDirectory.resolve("safe");
        List<String> safeSettings = List.of(PROFILE, fileUrl(safe),
                "spring.sql.init.schema-locations=classpath:jdbc03/schema-safe.sql",
                "spring.sql.init.data-locations=classpath:jdbc03/data.sql");
        try (ConfigurableApplicationContext first = start(safeSettings)) {
            assertThat(countProducts(first)).isEqualTo(3);
        }
        try (ConfigurableApplicationContext second = start(safeSettings)) {
            assertThat(countProducts(second)).as("tabela przeżyła, dane z data.sql dopisały się drugi raz").isEqualTo(6);
        }
    }

    @Test
    @DisplayName("Sekcja 4c: błąd w skrypcie danych zatrzymuje start, a continue-on-error=true go pomija")
    void section4cContinueOnError() {
        String errorScript = "spring.sql.init.data-locations=classpath:jdbc03/data-with-error.sql";
        assertThatThrownBy(() -> start(PROFILE, memUrl(), errorScript))
                .isInstanceOf(BeanCreationException.class)
                .hasCauseInstanceOf(ScriptStatementFailedException.class)
                .hasStackTraceContaining("Unique index or primary key violation");

        try (ConfigurableApplicationContext app = start(PROFILE, memUrl(), errorScript, "spring.sql.init.continue-on-error=true")) {
            assertThat(app.getBean(JdbcTemplate.class).queryForList("SELECT name FROM product ORDER BY id", String.class))
                    .containsExactly("Mysz", "Kubek");
        }
    }
}
