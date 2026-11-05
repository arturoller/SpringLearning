package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

/**
 * Dane testowe lekcji Tx03ProxyPitfalls. Testy działają BEZ transakcji testu, więc przed każdym przywracamy stan
 * z migracji V2 (także licznik id, żeby produkty zawsze miały id 1, 2, 3).
 */
public final class Tx03TestData {

    private Tx03TestData() {
    }

    public static void reset(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM product").update();
        jdbc.sql("ALTER TABLE product ALTER COLUMN id RESTART WITH 1").update();
        jdbc.sql("INSERT INTO product (name, price) VALUES ('Klawiatura', 100.00), ('Mysz', 50.00), ('Monitor', 800.00)")
                .update();
    }

    /** Produkty jako "nazwa=cena", posortowane po id. */
    public static List<String> products(JdbcClient jdbc) {
        return jdbc.sql("SELECT name || '=' || price FROM product ORDER BY id").query(String.class).list();
    }
}
