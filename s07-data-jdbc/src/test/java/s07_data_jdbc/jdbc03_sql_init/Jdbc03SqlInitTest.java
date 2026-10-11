package s07_data_jdbc.jdbc03_sql_init;

import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc03SqlInit.
 *
 * <p>Sekcja 1 używa pełnej aplikacji ({@code @SpringBootTest} + profil jdbc03). Pozostałe sekcje to doświadczenia na
 * małym kontekście (SqlInitSupport.start): taki kontekst NIE czyta application.properties,
 * więc pokazuje domyślne zachowanie Boota, a każdy test ma własną, świeżą bazę H2 o unikalnej nazwie.</p>
 */
@SpringBootTest
@ActiveProfiles("jdbc03")
class Jdbc03SqlInitTest {

    private static final String SIMPLE = "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-simple.sql";
    private static final String WITH_ERROR = "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-with-error.sql";

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("1. Najpierw schema.sql (tabela), potem data.sql (dane) - aplikacja startuje z gotowymi danymi")
    void schemaThenData() {
        List<String> names = jdbcClient.sql("SELECT name FROM categories ORDER BY name").query(String.class).list();
        assertThat(names).containsExactly("Nabial", "Napoje", "Slodycze");
    }

    @Test
    @DisplayName("2. Domyślny tryb to embedded (H2 dostaje skrypty), mode=never wyłącza skrypty")
    void modes() {
        try (var context = SqlInitSupport.start(SIMPLE)) {
            assertThat(SqlInitSupport.tableNames(context)).containsExactly("GADGETS");
        }
        try (var context = SqlInitSupport.start("spring.sql.init.mode=never", SIMPLE)) {
            assertThat(SqlInitSupport.tableNames(context)).isEmpty();
            assertThatThrownBy(() -> JdbcClient.create(context.getBean(DataSource.class))
                    .sql("SELECT * FROM gadgets").query().listOfRows())
                    .isInstanceOf(BadSqlGrammarException.class);
        }
    }

    @Test
    @DisplayName("3. Bez własnych locations Boot szuka schema.sql w korzeniu classpath - tu go nie ma, więc nic nie powstaje")
    void defaultLocationsAreRootOfClasspath() {
        try (var context = SqlInitSupport.start("spring.sql.init.mode=always")) {
            assertThat(SqlInitSupport.tableNames(context)).isEmpty();
        }
    }

    @Test
    @DisplayName("4. Dane bez schematu i błąd w skrypcie przerywają start; continue-on-error=true pomija błędną instrukcję")
    void failuresAndContinueOnError() {
        // same INSERT-y, brak tabeli categories
        assertThatThrownBy(() -> SqlInitSupport.start("spring.sql.init.data-locations=classpath:db/jdbc03/data.sql"))
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(JdbcSQLSyntaxErrorException.class)
                .hasStackTraceContaining(ScriptStatementFailedException.class.getSimpleName());

        // błąd w drugiej instrukcji skryptu
        assertThatThrownBy(() -> SqlInitSupport.start(WITH_ERROR))
                .hasStackTraceContaining(ScriptStatementFailedException.class.getSimpleName());

        // to samo z continue-on-error=true: kontekst startuje, powstają tabele z instrukcji poprawnych
        try (var context = SqlInitSupport.start("spring.sql.init.continue-on-error=true", WITH_ERROR)) {
            assertThat(SqlInitSupport.tableNames(context)).containsExactly("LEGACY_A", "LEGACY_B");
        }
    }

    @Test
    @DisplayName("5. Brak wersjonowania: drugi start na tej samej, trwałej bazie kończy się błędem 'table already exists'")
    void secondRunOnTheSameDatabaseFails() {
        String[] persistent = {
                "spring.datasource.url=jdbc:h2:mem:jdbc03_twice;DB_CLOSE_DELAY=-1",   // baza przeżywa zamknięcie kontekstu
                "spring.sql.init.mode=always", SIMPLE};

        try (var first = SqlInitSupport.start(persistent)) {
            assertThat(SqlInitSupport.tableNames(first)).containsExactly("GADGETS");
        }
        assertThatThrownBy(() -> SqlInitSupport.start(persistent))
                .hasRootCauseInstanceOf(JdbcSQLSyntaxErrorException.class)
                .rootCause().hasMessageContaining("already exists");

        // sprzątanie: trwała baza nie może zostać dla innych testów w tej samej JVM
        try (var cleanup = SqlInitSupport.start("spring.datasource.url=jdbc:h2:mem:jdbc03_twice;DB_CLOSE_DELAY=-1",
                "spring.sql.init.mode=never")) {
            JdbcClient.create(cleanup.getBean(DataSource.class)).sql("DROP ALL OBJECTS").update();
        }
    }
}
