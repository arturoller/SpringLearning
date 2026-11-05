package s08_data_jpa.solutions.jpa06_flyway_with_jpa;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa06FlywayWithJpa — zajrzyj dopiero po własnej próbie! */
public final class Jpa06FlywayWithJpaSolutions {

    private Jpa06FlywayWithJpaSolutions() {
    }

    /** Rozwiązanie 1: V + wersja + dwa podkreślniki + opis (spacje → podkreślniki) + .sql */
    public static String solution1MigrationFileName(int version, String description) {
        return "V" + version + "__" + description.trim().replace(' ', '_') + ".sql";
    }

    /** Rozwiązanie 2: information_schema.columns — kolejność kolumn to ordinal_position. */
    public static List<String> solution2Columns(DataSource dataSource, String table) {
        return JdbcClient.create(dataSource)
                .sql("SELECT LOWER(column_name) FROM information_schema.columns WHERE table_name = :table "
                        + "ORDER BY ordinal_position")
                .param("table", table)
                .query(String.class)
                .list();
    }

    /** Rozwiązanie 3: kolumna bez NOT NULL (może być pusta), potem dane. */
    public static List<String> solution3SupplierMigration() {
        return List.of(
                "ALTER TABLE product ADD COLUMN supplier VARCHAR(100)",
                "UPDATE product SET supplier = 'Nowak' WHERE category = 'COMPUTERS'");
    }

    /** Rozwiązanie 4: info().pending() — migracje znalezione w lokalizacji, ale jeszcze niewykonane na tej bazie. */
    public static List<String> solution4PendingVersions(DataSource dataSource) {
        MigrationInfo[] pending = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/jpa06")
                .load()
                .info()
                .pending();
        return Arrays.stream(pending).map(info -> info.getVersion().getVersion()).toList();
    }
}
