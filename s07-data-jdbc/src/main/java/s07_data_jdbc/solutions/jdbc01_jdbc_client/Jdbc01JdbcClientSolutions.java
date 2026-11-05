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

    /** Rozwiązanie 1: skalar COUNT(*) → Long, kategoria jako parametr (nie sklejona ze Stringiem). */
    public static long solution1CountInCategory(JdbcClient jdbc, String category) {
        return jdbc.sql("SELECT COUNT(*) FROM product WHERE category = :category")
                .param("category", category)
                .query(Long.class)
                .single();
    }

    /** Rozwiązanie 2: optional() zamiast single() — brak wiersza to Optional.empty(), nie wyjątek. */
    public static Optional<Product> solution2FindByName(JdbcClient jdbc, String name) {
        return jdbc.sql("SELECT * FROM product WHERE name = :name ORDER BY id")
                .param("name", name)
                .query(Product.class)
                .optional();
    }

    /** Rozwiązanie 3: wyrażenie stock + :delta liczy baza (atomowo), wynik update() to liczba wierszy. */
    public static int solution3IncreaseStock(JdbcClient jdbc, long id, int delta) {
        return jdbc.sql("UPDATE product SET stock = stock + :delta WHERE id = :id")
                .param("delta", delta)
                .param("id", id)
                .update();
    }

    /** Rozwiązanie 4: paramSource z rekordu + GeneratedKeyHolder z nazwą kolumny klucza. */
    public static long solution4InsertAndReturnId(JdbcClient jdbc, Product product) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO product (name, category, price, stock, discount_percent)
                        VALUES (:name, :category, :price, :stock, :discountPercent)""")
                .paramSource(product)
                .update(keyHolder, "id");
        return keyHolder.getKey().longValue();
    }

    /** Rozwiązanie 5: BETWEEN jest obustronnie włącznie; ORDER BY zapewnia deterministyczną kolejność. */
    public static List<String> solution5NamesInPriceRange(JdbcClient jdbc, BigDecimal min, BigDecimal max) {
        return jdbc.sql("SELECT name FROM product WHERE price BETWEEN :min AND :max ORDER BY price, name")
                .param("min", min)
                .param("max", max)
                .query(String.class)
                .list();
    }
}
