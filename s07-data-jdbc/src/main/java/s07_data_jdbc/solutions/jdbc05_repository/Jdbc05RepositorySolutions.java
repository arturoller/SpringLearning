package s07_data_jdbc.solutions.jdbc05_repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import s07_data_jdbc.jdbc05_repository.DuplicateSkuException;
import s07_data_jdbc.jdbc05_repository.Jdbc05Repository.CategoryCount;
import s07_data_jdbc.jdbc05_repository.NewProduct;
import s07_data_jdbc.jdbc05_repository.Product;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc05Repository — zajrzyj dopiero po własnej próbie! */
public final class Jdbc05RepositorySolutions {

    private Jdbc05RepositorySolutions() {
    }

    /** Rozwiązanie 1: warunek na cenie i sortowanie po cenie, a przy remisie po SKU. */
    public static List<Product> solution1(JdbcClient jdbc, BigDecimal maxPrice) {
        return jdbc.sql("SELECT id, sku, name, category, price, stock FROM product "
                        + "WHERE price <= :maxPrice ORDER BY price, sku")
                .param("maxPrice", maxPrice)
                .query(Product.class)
                .list();
    }

    /** Rozwiązanie 2: DELETE z warunkiem; update() zwraca liczbę usuniętych wierszy. */
    public static int solution2(JdbcClient jdbc) {
        return jdbc.sql("DELETE FROM product WHERE stock = 0").update();
    }

    /** Rozwiązanie 3: INSERT z kluczem generowanym; błąd UNIQUE zamieniony na wyjątek domenowy. */
    public static Product solution3(JdbcClient jdbc, NewProduct product) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.sql("INSERT INTO product (sku, name, category, price, stock) "
                            + "VALUES (:sku, :name, :category, :price, :stock)")
                    .paramSource(product)
                    .update(keyHolder, "id");
        } catch (DuplicateKeyException e) {
            throw new DuplicateSkuException(product.sku(), e);
        }
        return new Product(keyHolder.getKey().longValue(), product.sku(), product.name(), product.category(),
                product.price(), product.stock());
    }

    /** Rozwiązanie 4: GROUP BY, alias product_count mapuje się na pole productCount rekordu. */
    public static List<CategoryCount> solution4(JdbcClient jdbc) {
        return jdbc.sql("SELECT category, COUNT(*) AS product_count FROM product GROUP BY category ORDER BY category")
                .query(CategoryCount.class)
                .list();
    }
}
