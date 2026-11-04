package s11_testing.test03_mock_mvc_tester;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** Serwis produktów w pamięci: Kawa (1, 24,90) i Herbata (2, 15,99). {@link #reset()} przywraca ten stan. */
@Service
public class ProductService {

    private final ConcurrentMap<Long, ProductResponse> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public ProductService() {
        reset();
    }

    public final void reset() {
        products.clear();
        nextId.set(2);
        products.put(1L, new ProductResponse(1, "Kawa", new BigDecimal("24.90")));
        products.put(2L, new ProductResponse(2, "Herbata", new BigDecimal("15.99")));
    }

    /** Wszystkie produkty posortowane po id (kolejność w ConcurrentHashMap nie jest gwarantowana). */
    public List<ProductResponse> findAll() {
        return products.values().stream().sorted(Comparator.comparingLong(ProductResponse::id)).toList();
    }

    public ProductResponse get(long id) {
        ProductResponse product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public ProductResponse create(ProductRequest request) {
        long id = nextId.incrementAndGet();
        ProductResponse created = new ProductResponse(id, request.name(), request.price());
        products.put(id, created);
        return created;
    }
}
