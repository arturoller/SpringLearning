package s04_web_rest.rest05_crud_api;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serwis produktów — warstwa logiki biznesowej za kontrolerem (service = serwis, „usługa”). Dane są w pamięci
 * (bez bazy — baza od działu s07): {@code ConcurrentHashMap} jest bezpieczna przy wielu wątkach naraz (serwer obsługuje
 * zapytania równolegle), a {@code AtomicLong} nadaje kolejne id bez dwóch takich samych. Serwis NIC nie wie o HTTP:
 * zwraca {@code Optional} i {@code boolean}, a kody statusu ustala kontroler.
 * <p>
 * Dwa produkty startowe: Kawa (id 1) i Herbata (id 2). {@link #reset()} przywraca ten stan — testy wołają go przed
 * każdym przypadkiem, żeby nie zależały od kolejności.
 */
@Service
public class ProductService {

    private final ConcurrentMap<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public ProductService() {
        reset();
    }

    public final void reset() {
        products.clear();
        nextId.set(2);
        products.put(1L, newProduct(1, "Kawa", new BigDecimal("24.90")));
        products.put(2L, newProduct(2, "Herbata", new BigDecimal("15.99")));
    }

    /** Wszystkie produkty posortowane po id (kolejność w ConcurrentHashMap nie jest gwarantowana!). */
    public List<Product> findAll() {
        return products.values().stream().sorted(Comparator.comparingLong(Product::id)).toList();
    }

    public Optional<Product> find(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product create(ProductRequest request) {
        long id = nextId.incrementAndGet();
        Product product = newProduct(id, request.name(), request.price());
        products.put(id, product);
        return product;
    }

    /** Podmienia nazwę i cenę (PUT = pełna zamiana). Puste Optional, gdy produktu nie ma. */
    public Optional<Product> replace(long id, ProductRequest request) {
        // computeIfPresent jest atomowe: sprawdzenie „czy istnieje” i podmiana to jedna niepodzielna operacja
        return Optional.ofNullable(products.computeIfPresent(id,
                (key, old) -> new Product(key, request.name(), request.price(), old.internalCode())));
    }

    /** Zmienia tylko cenę (PATCH). Puste Optional, gdy produktu nie ma. */
    public Optional<Product> changePrice(long id, BigDecimal price) {
        return Optional.ofNullable(products.computeIfPresent(id,
                (key, old) -> new Product(key, old.name(), price, old.internalCode())));
    }

    /** @return true, jeśli produkt istniał i został usunięty; false, jeśli go nie było */
    public boolean delete(long id) {
        return products.remove(id) != null;
    }

    private static Product newProduct(long id, String name, BigDecimal price) {
        return new Product(id, name, price, "SKU-" + id);
    }
}
