package s07_data_jdbc.jdbc04_flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc04Flyway. Sekcja 1 używa pełnej aplikacji ({@code @SpringBootTest} + profil jdbc04: Flyway
 * wykonuje migracje z db/migration/jdbc04). Sekcje 2-4 wołają Flyway programowo (API) na świeżych bazach H2
 * z katalogów db/demo/... (src/test/resources), a sekcje 5-6 używają małego kontekstu ApplicationContextRunner.
 * Nic tu nie zmienia danych aplikacji, więc kolejność testów nie ma znaczenia.
 */
@SpringBootTest
@ActiveProfiles("jdbc04")
class Jdbc04FlywayTest {

    @Autowired
    JdbcClient jdbcClient;

    /** Świeża baza H2 w pamięci o unikalnej nazwie (każde wywołanie to osobna baza). */
    private static DataSource newDatabase() {
        return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
    }

    private static Flyway flyway(DataSource dataSource, String location) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:" + location).load();
    }

    @Test
    @DisplayName("1. Flyway wykonał migracje po kolei i zapisał je w tabeli flyway_schema_history")
    void appliedMigrationsAreInHistory() {
        // w H2 nazwy tabeli i kolumn Flyway'a są małymi literami w cudzysłowie
        List<Map<String, Object>> rows = jdbcClient.sql("""
                        SELECT "version", "description", "script", "success", "checksum"
                        FROM "flyway_schema_history"
                        WHERE "type" = 'SQL'
                        ORDER BY "installed_rank"
                        """)
                .query().listOfRows();

        assertThat(rows).extracting(row -> row.get("script")).containsExactly(
                "V1__create_products.sql", "V2__add_category.sql", "V3__seed_products.sql", "R__product_names_view.sql");
        assertThat(rows).extracting(row -> row.get("description"))     // podkreślniki z nazwy pliku stały się spacjami
                .containsExactly("create products", "add category", "seed products", "product names view");
        assertThat(rows).extracting(row -> row.get("version")).containsExactly("1", "2", "3", null);   // R__ nie ma wersji
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("success")).isEqualTo(true);
            assertThat(row.get("checksum")).isNotNull();            // suma kontrolna pliku
        });

        // efekt migracji: tabela z kolumną dodaną w V2, dane z V3 i widok z R__
        assertThat(jdbcClient.sql("SELECT name FROM product_names").query(String.class).list())
                .containsExactly("Czekolada", "Herbata", "Kawa");
        assertThat(jdbcClient.sql("SELECT category FROM products WHERE name = 'Kawa'").query(String.class).single())
                .isEqualTo("napoje");

        // pytanie kontrolne 8: bez cudzysłowów H2 szuka tabeli o DUŻYCH literach
        assertThatThrownBy(() -> jdbcClient.sql("SELECT version FROM flyway_schema_history").query().listOfRows())
                .isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    @DisplayName("2. Źle nazwane pliki (jeden podkreślnik, mała litera) są po cichu ignorowane; validateMigrationNaming to wykrywa")
    void namingRules() {
        DataSource db = newDatabase();
        Flyway flyway = flyway(db, "db/demo/naming");     // V1__ok, V2_single_underscore, v3__lowercase_prefix, V4__ok_again

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);      // wykonały się tylko V1 i V4
        assertThat(Arrays.stream(flyway.info().applied()).map(m -> m.getVersion().getVersion()).toList())
                .containsExactly("1", "4");

        Flyway strict = Flyway.configure().dataSource(newDatabase()).locations("classpath:db/demo/naming")
                .validateMigrationNaming(true).load();
        assertThatThrownBy(strict::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("V2_single_underscore.sql")
                .hasMessageContaining("v3__lowercase_prefix.sql");
    }

    @Test
    @DisplayName("3. Edycja wykonanej migracji zmienia sumę kontrolną i Flyway odmawia pracy (FlywayValidateException)")
    void checksumMismatch() {
        DataSource db = newDatabase();
        flyway(db, "db/demo/original").migrate();            // V1 wykonana w wersji oryginalnej

        Flyway edited = flyway(db, "db/demo/edited");        // ta sama wersja V1, ale z inną treścią pliku
        assertThatThrownBy(edited::validate)
                .isInstanceOf(FlywayValidateException.class)
                .hasMessageContaining("checksum mismatch for migration version 1");
        assertThatThrownBy(edited::migrate)                  // migrate też najpierw waliduje
                .isInstanceOf(FlywayValidateException.class);
    }

    @Test
    @DisplayName("4. Migracja powtarzalna (R__) wykonuje się ponownie po zmianie treści; bez zmian Flyway nie robi nic")
    void repeatableMigrationsAndIdempotence() {
        DataSource db = newDatabase();
        Flyway v1 = flyway(db, "db/demo/repeatable-v1");
        assertThat(v1.migrate().migrationsExecuted).isEqualTo(1);       // pierwsze wykonanie R__demo_view.sql
        assertThat(v1.migrate().migrationsExecuted).isZero();           // nic nowego: idempotencja

        Flyway v2 = flyway(db, "db/demo/repeatable-v2");                // ta sama nazwa pliku, zmieniona treść
        assertThat(v2.migrate().migrationsExecuted).isEqualTo(1);       // wykonana PONOWNIE, bez błędu sumy kontrolnej
        assertThat(JdbcClient.create(db).sql("SELECT n FROM demo_view").query(Integer.class).single()).isEqualTo(2);
        assertThat(Arrays.stream(v2.info().applied()).filter(m -> m.getVersion() == null).map(MigrationInfo::getScript).toList())
                .containsExactly("R__demo_view.sql", "R__demo_view.sql");   // dwa wpisy w historii
    }

    @Test
    @DisplayName("5. spring.flyway.enabled i spring.flyway.locations sterują Flyway w Bootcie")
    void bootProperties() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class));

        runner.withPropertyValues("spring.flyway.locations=classpath:db/migration/jdbc04").run(context -> {
            JdbcClient client = JdbcClient.create(context.getBean(DataSource.class));
            assertThat(client.sql("SELECT COUNT(*) FROM products").query(Integer.class).single()).isEqualTo(3);
        });

        runner.withPropertyValues("spring.flyway.enabled=false",
                "spring.flyway.locations=classpath:db/migration/jdbc04").run(context -> {
            assertThat(context).doesNotHaveBean(Flyway.class);
            JdbcClient client = JdbcClient.create(context.getBean(DataSource.class));
            assertThatThrownBy(() -> client.sql("SELECT COUNT(*) FROM products").query(Integer.class).single())
                    .isInstanceOf(BadSqlGrammarException.class);       // migracje nie biegły, więc tabeli nie ma
        });
    }

    @Test
    @DisplayName("6. Flyway + spring.sql.init na jednej bazie: Flyway biegnie pierwszy, a skrypt tworzący tę samą tabelę wywala start")
    void flywayAndSqlInitConflict() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class, DataSourceInitializationAutoConfiguration.class,
                        FlywayAutoConfiguration.class))
                .withPropertyValues("spring.flyway.locations=classpath:db/migration/jdbc04",
                        "spring.sql.init.mode=always",
                        "spring.sql.init.schema-locations=classpath:db/demo/conflict/schema.sql")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("already exists");
                });
    }
}
