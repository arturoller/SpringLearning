package s07_data_jdbc.solutions.jdbc05_repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc05Repository — zajrzyj dopiero po własnej próbie! */
public final class Jdbc05RepositorySolutions {

    private Jdbc05RepositorySolutions() {
    }

    static boolean solution1(JdbcClient jdbcClient, String name) {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM products WHERE name = :name")
                .param("name", name)
                .query(Integer.class)
                .single();
        return count > 0;
    }

    static long solution2(JdbcClient jdbcClient, String name, int priceCents) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("INSERT INTO products (name, price_cents) VALUES (:name, :price)")
                .param("name", name)
                .param("price", priceCents)
                .update(keyHolder, "id");
        return keyHolder.getKeyAs(Long.class);
    }

    static boolean solution3(JdbcClient jdbcClient, String name, int priceCents) {
        try {
            jdbcClient.sql("INSERT INTO products (name, price_cents) VALUES (:name, :price)")
                    .param("name", name)
                    .param("price", priceCents)
                    .update();
            return true;
        } catch (DuplicateKeyException e) {
            return false;   // nazwa zajęta - sytuacja oczekiwana; każdy inny wyjątek poleci wyżej
        }
    }
}
