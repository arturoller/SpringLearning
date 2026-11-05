package s07_data_jdbc.jdbc04_flyway;

/**
 * Jeden wiersz tabeli historii Flyway (flyway_schema_history) — niezmienny rekord. Nazwy pól odpowiadają kolumnom
 * (installedRank ↔ installed_rank). Migracja powtarzalna (R__…) nie ma wersji, więc {@code version} bywa {@code null}.
 */
public record AppliedMigration(Integer installedRank, String version, String description, String type,
                               String script, Integer checksum, Boolean success) {
}
