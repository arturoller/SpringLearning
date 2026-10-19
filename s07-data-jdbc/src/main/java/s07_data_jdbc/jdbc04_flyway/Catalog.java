package s07_data_jdbc.jdbc04_flyway;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

/** Zapytania o produkty — służą testom do sprawdzenia, co zrobiły migracje. */
@Component
public class Catalog {

    private final JdbcClient jdbc;

    public Catalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int count() {
        return jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single();
    }

    public List<String> skusOfExpensive() {
        return jdbc.sql("SELECT sku FROM expensive_product ORDER BY sku").query(String.class).list();
    }

    public int stockOf(String sku) {
        return jdbc.sql("SELECT stock FROM product WHERE sku = :sku").param("sku", sku).query(Integer.class).single();
    }
}
