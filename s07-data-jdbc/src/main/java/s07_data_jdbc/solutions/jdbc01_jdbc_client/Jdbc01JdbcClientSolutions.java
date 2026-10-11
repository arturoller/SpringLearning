package s07_data_jdbc.solutions.jdbc01_jdbc_client;

import org.springframework.jdbc.core.simple.JdbcClient;
import s07_data_jdbc.jdbc01_jdbc_client.Product;

import java.util.List;
import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc01JdbcClient — zajrzyj dopiero po własnej próbie! */
public final class Jdbc01JdbcClientSolutions {

    private Jdbc01JdbcClientSolutions() {
    }

    static List<String> solution1(JdbcClient jdbcClient, int maxPriceCents) {
        return jdbcClient.sql("SELECT name FROM products WHERE price_cents < :max ORDER BY name")
                .param("max", maxPriceCents)
                .query(String.class)
                .list();
    }

    static Optional<Product> solution2(JdbcClient jdbcClient, String name) {
        return jdbcClient.sql("SELECT id, name, price_cents, stock FROM products WHERE name = :name")
                .param("name", name)
                .query(Product.class)
                .optional();
    }

    static int solution3(JdbcClient jdbcClient, int percent) {
        return jdbcClient.sql("UPDATE products SET price_cents = price_cents * (100 - :percent) / 100")
                .param("percent", percent)
                .update();
    }
}
