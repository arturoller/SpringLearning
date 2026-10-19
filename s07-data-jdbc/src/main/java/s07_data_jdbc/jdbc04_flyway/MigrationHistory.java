package s07_data_jdbc.jdbc04_flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Pomocnik lekcji: pokazuje, co Flyway zapisał o migracjach. Bean {@code Flyway} (obiekt Flyway) tworzy autokonfiguracja
 * Boota z modułu spring-boot-flyway — wstrzykujemy go jak każdą zależność; JdbcClient czyta tabelę historii bezpośrednio.
 */
@Component
public class MigrationHistory {

    private final Flyway flyway;
    private final JdbcClient jdbc;

    public MigrationHistory(Flyway flyway, JdbcClient jdbc) {
        this.flyway = flyway;
        this.jdbc = jdbc;
    }

    /** Wiersze tabeli flyway_schema_history w kolejności wykonania: „rank | wersja | opis | typ | sukces”. */
    public List<String> describe() {
        return jdbc.sql("SELECT \"installed_rank\", \"version\", \"description\", \"type\", \"success\" "
                        + "FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"")
                .query((rs, rowNum) -> rs.getInt("installed_rank") + " | " + rs.getString("version") + " | "
                        + rs.getString("description") + " | " + rs.getString("type") + " | " + rs.getBoolean("success"))
                .list();
    }

    /** Wersje migracji wersjonowanych zapisane w historii (bez powtarzalnych — te mają wersję NULL). */
    public List<String> appliedVersions() {
        return Arrays.stream(flyway.info().applied())
                .map(MigrationInfo::getVersion)
                .filter(version -> version != null)
                .map(Object::toString)
                .toList();
    }

    /** Aktualna wersja schematu według Flyway (ostatnia wersjonowana migracja). */
    public String currentVersion() {
        MigrationInfo current = flyway.info().current();
        return current == null ? null : current.getVersion().toString();
    }

    /** Surowy bean Flyway — do prób z {@code clean()} w teście. */
    public Flyway flyway() {
        return flyway;
    }
}
