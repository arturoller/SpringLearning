package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Pomocnik testów ćwiczeń: świeża baza H2 w pamięci (unikalna nazwa) ze schematem i danymi lekcji Jdbc02JdbcTemplate —
 * bez kontekstu Springa, więc szybko i bez współdzielenia stanu między testami.
 */
public final class Jdbc02TestDatabase {

    private Jdbc02TestDatabase() {
    }

    public static EmbeddedDatabase create() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .addScript("classpath:db/jdbc02/schema.sql")
                .addScript("classpath:db/jdbc02/data.sql")
                .build();
    }
}
