package s08_data_jpa.jpa04_custom_queries;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * Podsłuch SQL dla testów lekcji Jpa04CustomQueries. StatementInspector (inspektor poleceń — interfejs Hibernate 7)
 * dostaje KAŻDE polecenie SQL tuż przed wysłaniem do bazy; my je tylko zapisujemy i zwracamy bez zmian.
 * Włączany właściwością {@code spring.jpa.properties.hibernate.session_factory.statement_inspector}.
 */
public class SqlCapture implements StatementInspector {

    private static final List<String> STATEMENTS = new ArrayList<>();

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql);
        return sql;
    }

    public static void clear() {
        STATEMENTS.clear();
    }

    /** Ostatnie wysłane polecenie (po zapytaniu w teście — właśnie ono). */
    public static String last() {
        return STATEMENTS.getLast();
    }

    public static List<String> all() {
        return List.copyOf(STATEMENTS);
    }
}
