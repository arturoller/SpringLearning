package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

/**
 * Dane testowe lekcji Tx02Propagation. Testy działają BEZ transakcji testu, więc przed każdym przywracamy stan
 * z migracji V2. Odczyty JdbcClientem spoza transakcji widzą tylko dane ZATWIERDZONE.
 */
public final class Tx02TestData {

    private Tx02TestData() {
    }

    public static void reset(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM shop_order").update();
        jdbc.sql("DELETE FROM audit_log").update();
        jdbc.sql("DELETE FROM stock").update();
        jdbc.sql("INSERT INTO stock (product, quantity) VALUES ('Klawiatura', 5), ('Mysz', 2)").update();
    }

    /** Zamówienia jako "numer:status", posortowane po numerze. */
    public static List<String> orders(JdbcClient jdbc) {
        return jdbc.sql("SELECT order_number || ':' || status FROM shop_order ORDER BY order_number")
                .query(String.class).list();
    }

    public static List<String> auditMessages(JdbcClient jdbc) {
        return jdbc.sql("SELECT message FROM audit_log ORDER BY id").query(String.class).list();
    }

    public static int stock(JdbcClient jdbc, String product) {
        return jdbc.sql("SELECT quantity FROM stock WHERE product = ?").param(product).query(Integer.class).single();
    }
}
