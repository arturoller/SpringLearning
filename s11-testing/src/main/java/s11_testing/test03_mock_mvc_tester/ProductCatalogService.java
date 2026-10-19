package s11_testing.test03_mock_mvc_tester;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Katalog produktów w pamięci ({@code ConcurrentHashMap} — mapa bezpieczna dla wielu wątków). Odczyt zawsze
 * posortowany po SKU, bo kolejność w mapie nie jest ustalona. W testach {@code @WebMvcTest} ten serwis to atrapa;
 * prawdziwy działa w teście z {@code RestTestClient} i po uruchomieniu aplikacji.
 */
@Service
public class ProductCatalogService {

    private final Map<String, ProductResponse> products = new ConcurrentHashMap<>();

    public ProductCatalogService() {
        add(new ProductResponse("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99")));
        add(new ProductResponse("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99")));
        add(new ProductResponse("SPO-002", "Czekolada gorzka", "SPOZYWCZE", new BigDecimal("7.49")));
        add(new ProductResponse("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00")));
        add(new ProductResponse("KSI-002", "Java. Podstawy", "KSIAZKI", new BigDecimal("129.00")));
    }

    private void add(ProductResponse product) {
        products.put(product.sku(), product);
    }

    /** Wszystkie produkty albo tylko z kategorii (gdy {@code category != null}), posortowane po SKU. */
    public List<ProductResponse> findAll(String category) {
        return products.values().stream()
                .filter(p -> category == null || p.category().equals(category))
                .sorted(Comparator.comparing(ProductResponse::sku))
                .toList();
    }

    public Optional<ProductResponse> findBySku(String sku) {
        return Optional.ofNullable(products.get(sku));
    }

    public ProductResponse create(NewProductRequest request) {
        var product = new ProductResponse(request.sku(), request.name(), request.category(), request.price());
        if (products.putIfAbsent(request.sku(), product) != null) {
            throw new DuplicateSkuException(request.sku());
        }
        return product;
    }

    /** true = usunięto; false = nie było takiego produktu. */
    public boolean delete(String sku) {
        return products.remove(sku) != null;
    }
}
