package s07_data_jdbc.solutions.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import s07_data_jdbc.jdbc01_jdbc_client.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc01JdbcClient — zajrzyj dopiero po własnej próbie! */
public final class Jdbc01JdbcClientSolutions {

    private Jdbc01JdbcClientSolutions() {
    }

    /** Rozwiązanie 1: kolumna jako lista napisów, sortowanie malejąco po cenie. */
    public static List<String> solution1(JdbcClient jdbc, String category) {
        return jdbc.sql("SELECT name FROM product WHERE category = :category ORDER BY price DESC")
                .param("category", category)
                .query(String.class)
                .list();
    }

    /** Rozwiązanie 2: LIMIT 1 + optional() — pusta kategoria daje Optional.empty(). */
    public static Optional<Product> solution2(JdbcClient jdbc, String category) {
        return jdbc.sql("SELECT * FROM product WHERE category = :category AND stock > 0 ORDER BY price LIMIT 1")
                .param("category", category)
                .query(Product.class)
                .optional();
    }

    /** Rozwiązanie 3: INSERT z odczytem klucza z kolumny id. */
    public static long solution3(JdbcClient jdbc, String sku, String name, BigDecimal price) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO product (sku, name, category, price, stock) VALUES (:sku, :name, 'DOM', :price, 0)")
                .param("sku", sku)
                .param("name", name)
                .param("price", price)
                .update(keyHolder, "id");
        return keyHolder.getKey().longValue();
    }

    /** Rozwiązanie 4: warunek w WHERE robi „sprawdź i zmień” atomowo; liczba wierszy mówi, czy się udało. */
    public static boolean solution4(JdbcClient jdbc, String sku, int amount) {
        int rows = jdbc.sql("UPDATE product SET stock = stock - :amount WHERE sku = :sku AND stock >= :amount")
                .param("amount", amount)
                .param("sku", sku)
                .update();
        return rows == 1;
    }
}
