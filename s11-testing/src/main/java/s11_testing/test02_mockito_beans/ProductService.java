package s11_testing.test02_mockito_beans;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serwis produktów w pamięci. Ma DWIE zależności do podmiany w testach: {@link PriceClient} (zewnętrzna usługa
 * cen — atrapa albo fake) i {@link AuditLog} (dziennik — często szpieg). Czas bierze z {@link Clock}.
 * Dwa produkty startowe: Kawa (1, 24,90) i Herbata (2, 15,99); {@link #reset()} przywraca ten stan.
 */
@Service
public class ProductService {

    private final PriceClient priceClient;
    private final AuditLog audit;
    private final Clock clock;
    private final ConcurrentMap<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public ProductService(PriceClient priceClient, AuditLog audit, Clock clock) {
        this.priceClient = priceClient;
        this.audit = audit;
        this.clock = clock;
        reset();
    }

    public final void reset() {
        products.clear();
        nextId.set(2);
        products.put(1L, new Product(1, "Kawa", new BigDecimal("24.90"), clock.instant()));
        products.put(2L, new Product(2, "Herbata", new BigDecimal("15.99"), clock.instant()));
    }

    public Product find(long id) {
        Product product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public Product create(ProductRequest request) {
        long id = nextId.incrementAndGet();
        Product product = new Product(id, request.name(), request.price(), clock.instant());
        products.put(id, product);
        audit.record("created:" + id);
        return product;
    }

    /**
     * Pyta zewnętrzną usługę o aktualną cenę i zapisuje ją. Gdy usługa nie odpowiada: cena zostaje stara,
     * w dzienniku ląduje "price-refresh-failed:ID", a wyjątek leci dalej.
     */
    public Product refreshPrice(long id) {
        Product old = find(id);
        BigDecimal fresh;
        try {
            fresh = priceClient.currentPrice(old.name());
        } catch (PriceUnavailableException e) {
            audit.record("price-refresh-failed:" + id);
            throw e;
        }
        Product updated = new Product(id, old.name(), fresh, clock.instant());
        products.put(id, updated);
        audit.record("price-refreshed:" + id);
        return updated;
    }
}
