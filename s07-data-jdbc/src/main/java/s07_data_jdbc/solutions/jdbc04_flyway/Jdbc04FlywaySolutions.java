package s07_data_jdbc.solutions.jdbc04_flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc04Flyway — zajrzyj dopiero po własnej próbie! */
public final class Jdbc04FlywaySolutions {

    private Jdbc04FlywaySolutions() {
    }

    static List<String> solution1(JdbcClient jdbcClient) {
        return jdbcClient.sql("""
                        SELECT "description" FROM "flyway_schema_history"
                        WHERE "version" IS NOT NULL
                        ORDER BY "installed_rank"
                        """)
                .query(String.class)
                .list();
    }

    static List<String> solution2(JdbcClient jdbcClient) {
        return jdbcClient.sql("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'PRODUCTS' ORDER BY COLUMN_NAME")
                .query(String.class)
                .list();
    }

    static List<String> solution3(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/jdbc04")
                .target("2")
                .load();
        flyway.migrate();
        return Arrays.stream(flyway.info().applied())
                .map(MigrationInfo::getScript)
                .filter(script -> !script.isEmpty())    // pomijamy wpis o utworzeniu tabeli historii
                .toList();
    }
}
