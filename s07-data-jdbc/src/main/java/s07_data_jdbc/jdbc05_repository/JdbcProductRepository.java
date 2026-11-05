package s07_data_jdbc.jdbc05_repository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementacja repozytorium na JdbcClient. Adnotacja {@code @Repository} (repository = repozytorium) oznacza
 * bean warstwy dostępu do danych: Spring może wokół niego włączyć tłumaczenie wyjątków (sekcja 2 lekcji).
 */
@Repository
public class JdbcProductRepository implements ProductRepository {

    private static final String COLUMNS = "id, name, category, price, stock, version";

    private final JdbcClient jdbc;

    public JdbcProductRepository(JdbcClient jdbc) { // wstrzykiwanie zależności przez konstruktor
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Product> findById(long id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM product WHERE id = :id")
                .param("id", id)
                .query(Product.class)
                .optional();
    }

    @Override
    public List<Product> findAll() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM product ORDER BY id")
                .query(Product.class)
                .list();
    }

    @Override
    public List<Product> findByCategory(String category) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM product WHERE category = :category ORDER BY id")
                .param("category", category)
                .query(Product.class)
                .list();
    }

    @Override
    public Product save(Product product) {
        return product.id() == null ? insert(product) : update(product);
    }

    private Product insert(Product product) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO product (name, category, price, stock)
                        VALUES (:name, :category, :price, :stock)""")
                .param("name", product.name())
                .param("category", product.category())
                .param("price", product.price())
                .param("stock", product.stock())
                .update(keyHolder, "id");
        long id = keyHolder.getKey().longValue();
        return new Product(id, product.name(), product.category(), product.price(), product.stock(), 0);
    }

    /** Optymistyczne blokowanie: UPDATE trafia w wiersz tylko wtedy, gdy nadal ma wersję, którą wczytaliśmy. */
    private Product update(Product product) {
        int rows = jdbc.sql("""
                        UPDATE product
                        SET name = :name, category = :category, price = :price, stock = :stock, version = version + 1
                        WHERE id = :id AND version = :version""")
                .param("name", product.name())
                .param("category", product.category())
                .param("price", product.price())
                .param("stock", product.stock())
                .param("id", product.id())
                .param("version", product.version())
                .update();
        if (rows == 0) { // 0 wierszy = ktoś zmienił (albo usunął) produkt po naszym odczycie
            throw new OptimisticLockingFailureException(
                    "Produkt " + product.id() + " został zmieniony lub usunięty (nasza wersja: " + product.version() + ")");
        }
        return new Product(product.id(), product.name(), product.category(), product.price(), product.stock(),
                product.version() + 1);
    }

    @Override
    public boolean deleteById(long id) {
        return jdbc.sql("DELETE FROM product WHERE id = :id").param("id", id).update() == 1;
    }
}
