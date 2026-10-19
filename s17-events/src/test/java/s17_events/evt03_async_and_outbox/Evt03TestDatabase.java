package s17_events.evt03_async_and_outbox;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Pomocnik testów ćwiczeń: świeża baza H2 w pamięci (unikalna nazwa) ze schematem z migracji V1 lekcji
 * (skrypt wykonany bezpośrednio, bez Flyway i bez kontekstu Springa — szybko i bez współdzielenia stanu).
 */
public final class Evt03TestDatabase {

    private Evt03TestDatabase() {
    }

    public static EmbeddedDatabase create() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .addScript("classpath:db/migration/evt03/V1__create_tables.sql")
                .build();
    }
}
