package s07_data_jdbc.jdbc05_repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium (repository = klasa odpowiedzialna za dostęp do danych jednej tabeli/encji) produktów na JdbcClient.
 *
 * <p>{@code @Repository} (repozytorium) to stereotyp ze Springa: oznacza warstwę dostępu do danych i sprawia, że klasa
 * jest beanem znajdowanym przez skanowanie. Zależność (JdbcClient) przychodzi przez konstruktor.</p>
 */
@Repository
public class ProductRepository {

    private final JdbcClient jdbcClient;

    public ProductRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Wstawia produkt i zwraca go z identyfikatorem nadanym przez bazę (klucz generowany). */
    public Product insert(String name, int priceCents) {
        KeyHolder keyHolder = new GeneratedKeyHolder();   // key holder = "pojemnik na klucz" wypełniany przez bazę
        jdbcClient.sql("INSERT INTO products (name, price_cents) VALUES (:name, :price)")
                .param("name", name)
                .param("price", priceCents)
                .update(keyHolder, "id");                  // "id" = kolumna, której wartość chcemy odzyskać
        long id = keyHolder.getKeyAs(Long.class);
        return new Product(id, name, priceCents);
    }

    /** Szuka po id; brak produktu to Optional.empty(), nie wyjątek. */
    public Optional<Product> findById(long id) {
        return jdbcClient.sql("SELECT id, name, price_cents FROM products WHERE id = :id")
                .param("id", id)
                .query(Product.class)
                .optional();
    }

    /** Jak findById, ale brak produktu oznacza błąd domenowy (gdy wołający "wie", że produkt musi istnieć). */
    public Product getById(long id) {
        return findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** Wszystkie produkty posortowane po nazwie (jawne ORDER BY - bez niego kolejność nie jest gwarantowana). */
    public List<Product> findAll() {
        return jdbcClient.sql("SELECT id, name, price_cents FROM products ORDER BY name")
                .query(Product.class)
                .list();
    }

    /** Zmienia cenę; zwraca true, gdy produkt istniał (zmieniono dokładnie 1 wiersz). */
    public boolean updatePrice(long id, int newPriceCents) {
        int changed = jdbcClient.sql("UPDATE products SET price_cents = :price WHERE id = :id")
                .param("price", newPriceCents)
                .param("id", id)
                .update();
        return changed == 1;
    }

    /** Usuwa produkt; zwraca true, gdy coś usunięto. */
    public boolean deleteById(long id) {
        return jdbcClient.sql("DELETE FROM products WHERE id = :id").param("id", id).update() == 1;
    }
}
