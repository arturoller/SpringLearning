package s07_data_jdbc.solutions.jdbc04_flyway;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Locale;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc04Flyway — zajrzyj dopiero po własnej próbie! */
public final class Jdbc04FlywaySolutions {

    private Jdbc04FlywaySolutions() {
    }

    /** Rozwiązanie 1: V + wersja + DWA podkreślniki + opis (spacje → podkreślniki, małe litery) + .sql. */
    public static String solution1MigrationFileName(int version, String description) {
        String words = description.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        return "V" + version + "__" + words + ".sql";
    }

    /**
     * Rozwiązanie 2: sortowanie po installed_rank (kolejność wykonania), nie po tekście wersji — w tekście
     * "9" jest większe od "10". Nazwy tabeli i kolumn w cudzysłowie, bo Flyway tworzy je małymi literami.
     */
    public static String solution2LastVersion(JdbcClient jdbc) {
        return jdbc.sql("""
                        SELECT "version" FROM "flyway_schema_history"
                        WHERE "success" = TRUE AND "version" IS NOT NULL
                        ORDER BY "installed_rank" DESC
                        LIMIT 1""")
                .query(String.class)
                .single();
    }

    /** Rozwiązanie 3: ALTER TABLE ADD COLUMN bez NOT NULL — istniejące wiersze dostają NULL. */
    public static String solution3AddDiscountSql() {
        return "ALTER TABLE product ADD COLUMN discount_percent INT;";
    }

    /** Rozwiązanie 4: CREATE OR REPLACE VIEW — da się wykonać wielokrotnie, więc pasuje do migracji R__. */
    public static String solution4ExpensiveProductViewSql() {
        return """
                CREATE OR REPLACE VIEW expensive_product AS
                SELECT name, price FROM product WHERE price >= 100;
                """;
    }
}
