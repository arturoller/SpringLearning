package s07_data_jdbc.jdbc04_flyway;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

/**
 * Odczyt tabeli historii Flyway przez JdbcClient. UWAGA NA CUDZYSŁOWY: Flyway tworzy w H2 tabelę i kolumny
 * z MAŁYMI literami w cudzysłowie ({@code "flyway_schema_history"}), a H2 bez cudzysłowu zamienia nazwy na WIELKIE
 * litery — zapytanie {@code SELECT * FROM flyway_schema_history} kończy się więc błędem „table not found”.
 */
public final class Jdbc04MigrationHistory {

    private Jdbc04MigrationHistory() {
    }

    /** Migracje SQL zapisane w historii, w kolejności wykonania (bez wiersza „tabela historii utworzona”). */
    public static List<AppliedMigration> sqlMigrations(JdbcClient jdbc) {
        return jdbc.sql("""
                        SELECT "installed_rank", "version", "description", "type", "script", "checksum", "success"
                        FROM "flyway_schema_history"
                        WHERE "type" = 'SQL'
                        ORDER BY "installed_rank\"""")
                .query(AppliedMigration.class)
                .list();
    }
}
