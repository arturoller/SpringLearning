package s04_web_rest.rest03_responses;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Magazyn produktów w pamięci (bez bazy — baza od działu s07). Dwa produkty startowe: Kawa (id 1) i Herbata (id 2).
 * Metoda {@link #reset()} przywraca stan początkowy — testy wołają ją przed każdym przypadkiem, żeby nie zależały od kolejności.
 */
@Component
public class ProductStore {

    private final ConcurrentMap<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public ProductStore() {
        reset();
    }

    public final void reset() {
        products.clear();
        nextId.set(2);
        products.put(1L, new Product(1, "Kawa", 2499));
        products.put(2L, new Product(2, "Herbata", 1599));
    }

    public Optional<Product> find(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public List<Product> all() {
        return products.values().stream().sorted(Comparator.comparingLong(Product::id)).toList();
    }

    public boolean existsByName(String name) {
        return products.values().stream().anyMatch(product -> product.name().equalsIgnoreCase(name));
    }

    public Product add(String name, long priceGrosze) {
        Product product = new Product(nextId.incrementAndGet(), name, priceGrosze);
        products.put(product.id(), product);
        return product;
    }

    public boolean remove(long id) {
        return products.remove(id) != null;
    }
}
