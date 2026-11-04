package s10_transactions;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * Podsłuch SQL dla testów działu s10_transactions (kopia z s09_jpa_relations; opis: s08_data_jpa/Jpa04CustomQueries). StatementInspector
 * (inspektor poleceń — interfejs Hibernate 7) dostaje KAŻDE polecenie SQL tuż przed wysłaniem do bazy; my je tylko
 * zapisujemy i zwracamy bez zmian. Włączany właściwością
 * {@code spring.jpa.properties.hibernate.session_factory.statement_inspector=s10_transactions.SqlCapture}
 * (stała {@link #PROPERTY}). Klasa leży w pakiecie działu, bo korzystają z niej testy wszystkich lekcji.
 */
public class SqlCapture implements StatementInspector {

    /** Gotowa właściwość do {@code @DataJpaTest(properties = SqlCapture.PROPERTY)}. */
    public static final String PROPERTY =
            "spring.jpa.properties.hibernate.session_factory.statement_inspector=s10_transactions.SqlCapture";

    private static final List<String> STATEMENTS = new ArrayList<>();

    @Override
    public String inspect(String sql) {
        synchronized (STATEMENTS) {
            STATEMENTS.add(sql);
        }
        return sql;
    }

    public static void clear() {
        synchronized (STATEMENTS) {
            STATEMENTS.clear();
        }
    }

    /** Ostatnie wysłane polecenie. */
    public static String last() {
        synchronized (STATEMENTS) {
            return STATEMENTS.getLast();
        }
    }

    public static List<String> all() {
        synchronized (STATEMENTS) {
            return List.copyOf(STATEMENTS);
        }
    }

    /** Polecenia zaczynające się od danego słowa (np. "select", "insert", "delete"), bez względu na wielkość liter. */
    public static List<String> startingWith(String keyword) {
        return all().stream().filter(sql -> sql.regionMatches(true, 0, keyword, 0, keyword.length())).toList();
    }
}
