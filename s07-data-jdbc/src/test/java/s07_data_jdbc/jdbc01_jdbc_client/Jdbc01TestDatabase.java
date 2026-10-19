package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Pomocnik testów ćwiczeń: świeża baza H2 w pamięci (z unikalną nazwą) ze schematem i danymi lekcji — bez kontekstu
 * Springa, więc szybko i bez współdzielenia stanu między testami.
 */
public final class Jdbc01TestDatabase {

    private Jdbc01TestDatabase() {
    }

    public static EmbeddedDatabase create() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .addScript("classpath:db/jdbc01/schema.sql")
                .addScript("classpath:db/jdbc01/data.sql")
                .build();
    }

    public static JdbcClient client(EmbeddedDatabase database) {
        return JdbcClient.create(database);
    }
}
