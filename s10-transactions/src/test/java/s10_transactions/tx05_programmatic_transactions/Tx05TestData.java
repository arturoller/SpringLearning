package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

/**
 * Dane testowe lekcji Tx05ProgrammaticTransactions. Testy działają BEZ transakcji testu, więc przed każdym czyścimy
 * tabele (stan z migracji: puste). Odczyty JdbcClientem spoza transakcji widzą tylko dane ZATWIERDZONE.
 */
public final class Tx05TestData {

    private Tx05TestData() {
    }

    public static void reset(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM customer_order").update();
        jdbc.sql("DELETE FROM audit_log").update();
    }

    /** Numery zapisanych zamówień, posortowane. */
    public static List<String> orders(JdbcClient jdbc) {
        return jdbc.sql("SELECT order_number FROM customer_order ORDER BY order_number").query(String.class).list();
    }

    public static List<String> auditMessages(JdbcClient jdbc) {
        return jdbc.sql("SELECT message FROM audit_log ORDER BY id").query(String.class).list();
    }
}
