package s07_data_jdbc.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc03SqlInit — „wynik” każdej sekcji.
 *
 * <p>Zwykły {@code @SpringBootTest} tu nie wystarcza, bo każdy scenariusz potrzebuje INNYCH ustawień i świeżej bazy
 * (tryb, lokalizacje skryptów, błąd przy starcie). Dlatego test sam uruchamia aplikację lekcji przez pomocnika
 * Jdbc03TestSupport — za każdym razem z własnym zestawem argumentów i bazą o unikalnej nazwie.</p>
 */
class Jdbc03SqlInitTest {

    @Test
    @DisplayName("1. Boot wykonuje schema.sql, potem data.sql — tabela ma 7 pracowników")
    void section1() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start()) {
            EmployeeQueries queries = context.getBean(EmployeeQueries.class);
            assertThat(queries.count()).isEqualTo(7);
            assertThat(queries.namesOf("IT")).containsExactly("Anna Nowak", "Michał Lewandowski", "Piotr Kowalski");
        }
    }

    @Test
    @DisplayName("2. Skrypty z listy wykonują się w podanej kolejności; zła kolejność przerywa start")
    void section2() {
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(
                "spring.sql.init.schema-locations=classpath:db/jdbc03/schema.sql,classpath:db/jdbc03/schema-views.sql")) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("it_employee")).isTrue();
        }

        assertThatThrownBy(() -> Jdbc03TestSupport.start(
                "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-views.sql,classpath:db/jdbc03/schema.sql"))
                .hasStackTraceContaining("ScriptStatementFailedException")
                .hasStackTraceContaining(
                        "Failed to execute SQL script statement #1 of class path resource [db/jdbc03/schema-views.sql]");
    }

    @Test
    @DisplayName("3. Tryb embedded nie dotyka bazy w pliku, always wykonuje skrypty, never nie wykonuje nigdy")
    void section3(@TempDir Path tempDir) {
        String fileUrl = "jdbc:h2:file:" + tempDir.resolve("sklep").toString().replace('\\', '/');

        // domyślny tryb (embedded): H2 w PLIKU nie jest dla Boota bazą wbudowaną → skryptów brak
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startOn(fileUrl)) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("employee")).isFalse();
        }
        // always: skrypty wykonane mimo bazy w pliku
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startOn(fileUrl, "spring.sql.init.mode=always")) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(7);
        }
        // never: baza w pamięci, ale bez skryptów → tabeli nie ma, zapytanie kończy się błędem
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start("spring.sql.init.mode=never")) {
            EmployeeQueries queries = context.getBean(EmployeeQueries.class);
            assertThat(queries.tableExists("employee")).isFalse();
            assertThatThrownBy(queries::count).isInstanceOf(BadSqlGrammarException.class);
        }
    }

    @Test
    @DisplayName("4. Błąd w skrypcie zatrzymuje start, a continue-on-error=true go pomija")
    void section4() {
        String brokenData = "spring.sql.init.data-locations=classpath:db/jdbc03/data-with-error.sql";

        assertThatThrownBy(() -> Jdbc03TestSupport.start(brokenData))
                .hasStackTraceContaining(
                        "Failed to execute SQL script statement #2 of class path resource [db/jdbc03/data-with-error.sql]");

        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start(
                brokenData, "spring.sql.init.continue-on-error=true")) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(2);   // 1. i 3. INSERT przeszły
        }
    }

    @Test
    @DisplayName("5. Plik schema-PLATFORMA.sql jest czytany tylko przy domyślnych lokalizacjach i ustawionej platformie")
    void section5() {
        // bez własnych schema-locations i z platform=jdbc03platform: Boot czyta schema-jdbc03platform.sql z korzenia classpath
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startWithoutProfile(
                "spring.sql.init.platform=jdbc03platform")) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("platform_marker")).isTrue();
        }
        // ta sama aplikacja, ale bez ustawionej platformy (domyślnie "all"): pliku platformowego nie ma w grze
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startWithoutProfile()) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("platform_marker")).isFalse();
        }
        // platforma ustawiona, ale własne schema-locations (profil jdbc03) wyłączają szukanie plików platformowych
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.start("spring.sql.init.platform=jdbc03platform")) {
            assertThat(context.getBean(EmployeeQueries.class).tableExists("platform_marker")).isFalse();
        }
    }

    @Test
    @DisplayName("6. Drugi start na tej samej bazie: CREATE TABLE pada, a z IF NOT EXISTS dane się dublują")
    void section6() {
        // DB_CLOSE_DELAY=-1: baza przeżywa zamknięcie kontekstu — to symulacja restartu aplikacji z trwałą bazą
        String restartUrl = "jdbc:h2:mem:jdbc03_restart;DB_CLOSE_DELAY=-1";
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startOn(restartUrl)) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(7);
        }
        assertThatThrownBy(() -> Jdbc03TestSupport.startOn(restartUrl))   // „restart”: tabela już istnieje
                .hasStackTraceContaining("ScriptStatementFailedException")
                .hasRootCauseInstanceOf(SQLException.class);

        String idempotentUrl = "jdbc:h2:mem:jdbc03_idempotent;DB_CLOSE_DELAY=-1";
        String idempotentSchema = "spring.sql.init.schema-locations=classpath:db/jdbc03/schema-idempotent.sql";
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startOn(idempotentUrl, idempotentSchema)) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(7);
        }
        try (ConfigurableApplicationContext context = Jdbc03TestSupport.startOn(idempotentUrl, idempotentSchema)) {
            assertThat(context.getBean(EmployeeQueries.class).count()).isEqualTo(14);   // dane wstawione drugi raz
        }
    }
}
