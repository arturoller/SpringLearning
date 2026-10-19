package s04_web_rest.rest05_crud_api;

import org.springframework.stereotype.Repository;
import s04_web_rest.rest05_crud_api.Product.Category;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductRepository = repozytorium produktów W PAMIĘCI. {@code @Repository} (repozytorium) — stereotyp oznaczający
 * rolę „dostęp do danych” (s01_core_ioc/Ioc03Stereotypes); prawdziwa baza danych — od działu s07_data_jdbc.
 *
 * <p>ConcurrentHashMap (mapa bezpieczna dla wielu wątków), bo repozytorium jest singletonem, a Tomcat obsługuje
 * zapytania równolegle. Mapa nie gwarantuje kolejności — findAll() SORTUJE przy odczycie, żeby lista zawsze była
 * taka sama (ważne dla klientów API i dla testów).</p>
 */
@Repository
public class ProductRepository {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public ProductRepository() {
        save(new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"),
                new BigDecimal("4300.00"), 7));
        save(new Product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, new BigDecimal("2999.00"),
                new BigDecimal("2350.00"), 0));
        save(new Product("KSI-001", "Czysty kod", Category.KSIAZKI, new BigDecimal("79.00"),
                new BigDecimal("52.00"), 15));
        save(new Product("SPO-001", "Kawa ziarnista 1kg", Category.SPOZYWCZE, new BigDecimal("64.99"),
                new BigDecimal("41.00"), 120));
    }

    public List<Product> findAll() {
        return products.values().stream()
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    public Optional<Product> findBySku(String sku) {
        return Optional.ofNullable(products.get(sku));
    }

    public boolean existsBySku(String sku) {
        return products.containsKey(sku);
    }

    /** Zapisuje produkt (nowy albo zastępuje istniejący o tym samym sku) i go zwraca. */
    public Product save(Product product) {
        products.put(product.sku(), product);
        return product;
    }

    /** Dodaje produkt tylko wtedy, gdy sku jest wolny — atomowo (putIfAbsent). Zwraca true, gdy dodano. */
    public boolean saveIfAbsent(Product product) {
        return products.putIfAbsent(product.sku(), product) == null;
    }

    /** Zastępuje produkt tylko wtedy, gdy już istnieje — atomowo (Map.replace). Zwraca true, gdy zastąpiono. */
    public boolean replaceIfPresent(Product product) {
        return products.replace(product.sku(), product) != null;
    }

    /** Usuwa produkt; true — gdy był, false — gdy go nie było. */
    public boolean deleteBySku(String sku) {
        return products.remove(sku) != null;
    }
}
