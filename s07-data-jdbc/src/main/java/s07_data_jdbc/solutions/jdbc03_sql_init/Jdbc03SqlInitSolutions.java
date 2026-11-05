package s07_data_jdbc.solutions.jdbc03_sql_init;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc03SqlInit — zajrzyj dopiero po własnej próbie! */
public final class Jdbc03SqlInitSolutions {

    private Jdbc03SqlInitSolutions() {
    }

    /** Rozwiązanie 1: tryb "always" wykonuje skrypty niezależnie od rodzaju bazy. */
    public static String solution1ModeProperty() {
        return "spring.sql.init.mode=always";
    }

    /** Rozwiązanie 2: skrypt z CREATE TABLE IF NOT EXISTS można uruchamiać wielokrotnie na tej samej bazie. */
    public static String solution2RepeatableSchemaLocation() {
        return "classpath:jdbc03/schema-safe.sql";
    }

    /** Rozwiązanie 3: błędny skrypt danych + pomijanie błędnych poleceń. */
    public static List<String> solution3ContinueOnErrorProperties() {
        return List.of(
                "spring.sql.init.data-locations=classpath:jdbc03/data-with-error.sql",
                "spring.sql.init.continue-on-error=true");
    }

    /** Rozwiązanie 4: trzy polecenia oddzielone średnikami — tak samo wykonałoby je @Sql. */
    public static String solution4Script() {
        return """
                INSERT INTO product (name, category, price, stock) VALUES ('Zeszyt', 'dom', 4.50, 40);
                INSERT INTO product (name, category, price, stock) VALUES ('Teczka', 'dom', 7.00, 10);
                UPDATE product SET stock = 7 WHERE name = 'Lampka';
                """;
    }

    /** Rozwiązanie 5: platforma dokłada schema-jdbc03platform.sql i data-jdbc03platform.sql (z korzenia classpath). */
    public static String solution5PlatformProperty() {
        return "spring.sql.init.platform=jdbc03platform";
    }
}
