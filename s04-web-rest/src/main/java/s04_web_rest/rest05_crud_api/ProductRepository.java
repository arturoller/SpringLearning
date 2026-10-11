package s04_web_rest.rest05_crud_api;

import org.springframework.stereotype.Repository;
import s04_web_rest.rest05_crud_api.CrudModels.Product;
import s04_web_rest.rest05_crud_api.CrudModels.ProductPatch;
import s04_web_rest.rest05_crud_api.CrudModels.ProductRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ProductRepository = repozytorium (magazyn) produktów W PAMIĘCI, bezpieczne dla wielu wątków.
 * {@code @Repository} (repozytorium) — stereotyp warstwy danych; to zwykły bean. Prawdziwa baza — od s07_data_jdbc.
 * <p>
 * Dlaczego bezpieczne? Tomcat obsługuje każde zapytanie w osobnym wątku, a bean jest JEDEN (singleton):
 * <ul>
 *   <li>ConcurrentHashMap — mapa współbieżna: wiele wątków może czytać i pisać naraz bez psucia mapy;</li>
 *   <li>AtomicLong.getAndIncrement() — licznik id niepodzielny: dwa POST-y w tej samej chwili nie dostaną tego samego id;</li>
 *   <li>computeIfPresent — „sprawdź, czy jest, i zmień” jako JEDNA niepodzielna operacja (bez wyścigu między get a put).</li>
 * </ul>
 * Zobacz t21_concurrency/Concurrency06ConcurrentCollections.
 */
@Repository
public class ProductRepository {

    private final ConcurrentHashMap<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ProductRepository() {
        resetToSampleData();
    }

    /** Lista posortowana po id — ConcurrentHashMap NIE gwarantuje kolejności, więc sortujemy jawnie. */
    public List<Product> findAll() {
        return products.values().stream().sorted(Comparator.comparingLong(Product::id)).toList();
    }

    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product create(ProductRequest request) {
        long id = nextId.getAndIncrement();
        Product product = new Product(id, request.name(), request.priceGrosze());
        products.put(id, product);
        return product;
    }

    /** PUT: zastąp CAŁY produkt, jeśli istnieje. Pusty Optional = nie było czego zastąpić. */
    public Optional<Product> replace(long id, ProductRequest request) {
        return Optional.ofNullable(products.computeIfPresent(id,
                (key, old) -> new Product(id, request.name(), request.priceGrosze())));
    }

    /** PATCH: zmień tylko pola różne od null. Pusty Optional = brak produktu. */
    public Optional<Product> patch(long id, ProductPatch patch) {
        return Optional.ofNullable(products.computeIfPresent(id, (key, old) -> new Product(
                id,
                patch.name() != null ? patch.name() : old.name(),
                patch.priceGrosze() != null ? patch.priceGrosze() : old.priceGrosze())));
    }

    /** @return true, gdy produkt był i został usunięty */
    public boolean delete(long id) {
        return products.remove(id) != null;
    }

    /** Przywraca dane startowe: Kawa (1), Herbata (2), Czekolada (3). Używane przy starcie i w testach. */
    final void resetToSampleData() {
        products.clear();
        nextId.set(1);
        create(new ProductRequest("Kawa", 2999));
        create(new ProductRequest("Herbata", 1599));
        create(new ProductRequest("Czekolada", 899));
    }
}
