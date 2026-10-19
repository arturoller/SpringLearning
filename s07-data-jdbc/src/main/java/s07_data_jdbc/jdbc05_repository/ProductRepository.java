package s07_data_jdbc.jdbc05_repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repozytorium (repository = „magazyn danych”, jedyne miejsce, które zna SQL) produktów, zbudowane na {@code JdbcClient}.
 * Adnotacja {@code @Repository} to stereotyp (lekcja Ioc03Stereotypes): bean wykrywany przez skanowanie, a w pełnym
 * kontekście aplikacji dodatkowo obejmowany tłumaczeniem wyjątków trwałości (patrz test sekcji 6).
 */
@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAll() {
        return jdbc.sql("SELECT id, sku, name, category, price, stock FROM product ORDER BY id")
                .query(Product.class)
                .list();
    }

    /** Produktu może nie być → {@code Optional}, a nie wyjątek i nie {@code null}. */
    public Optional<Product> findBySku(String sku) {
        return jdbc.sql("SELECT id, sku, name, category, price, stock FROM product WHERE sku = :sku")
                .param("sku", sku)
                .query(Product.class)
                .optional();
    }

    public List<Product> findByCategory(String category) {
        return jdbc.sql("SELECT id, sku, name, category, price, stock FROM product WHERE category = :category ORDER BY sku")
                .param("category", category)
                .query(Product.class)
                .list();
    }

    public int count() {
        return jdbc.sql("SELECT COUNT(*) FROM product").query(Integer.class).single();
    }

    /**
     * Zapisuje nowy produkt i zwraca go już z id nadanym przez bazę. Unikalność SKU pilnuje BAZA (ograniczenie UNIQUE),
     * a repozytorium tłumaczy błąd na wyjątek domenowy {@link DuplicateSkuException}.
     */
    public Product save(NewProduct product) {
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

    /** Zwraca {@code true}, jeśli produkt istniał i cena została zmieniona. */
    public boolean updatePrice(String sku, BigDecimal newPrice) {
        int rows = jdbc.sql("UPDATE product SET price = :price WHERE sku = :sku")
                .param("price", newPrice)
                .param("sku", sku)
                .update();
        return rows == 1;
    }

    /** Zwraca {@code true}, jeśli produkt istniał i został usunięty. */
    public boolean deleteBySku(String sku) {
        return jdbc.sql("DELETE FROM product WHERE sku = :sku").param("sku", sku).update() == 1;
    }

    /**
     * ŹLE — sprawdź, potem wstaw (check-then-act): między SELECT a INSERT może wejść inny użytkownik z tym samym SKU.
     * Zostawione tylko dla porównania (patrz PUŁAPKA w lekcji); prawdziwą ochroną jest ograniczenie UNIQUE w bazie.
     */
    public boolean existsBySku(String sku) {
        return jdbc.sql("SELECT COUNT(*) FROM product WHERE sku = :sku")
                .param("sku", sku)
                .query(Integer.class)
                .single() > 0;
    }
}
