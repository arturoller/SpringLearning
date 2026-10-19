package s22_extras.ext05_webflux_overview;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Katalog „reaktywny” (lekcja Ext05WebFluxOverview). Zwraca Mono/Flux zamiast obiektów i list — tak wygląda API
 * reaktywnego repozytorium (np. Spring Data R2DBC albo MongoDB Reactive). Tu dane są w pamięci.
 */
@Component
public class ReactiveCatalog {

    private final List<ReactiveProduct> products = List.of(
            new ReactiveProduct("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7),
            new ReactiveProduct("ELE-002", "Smartfon X", new BigDecimal("2999.00"), 0),
            new ReactiveProduct("ELE-003", "Słuchawki BT", new BigDecimal("349.90"), 25),
            new ReactiveProduct("SPO-001", "Kawa ziarnista 1kg", new BigDecimal("64.99"), 120));

    /** Flux = strumień 0..N elementów. */
    public Flux<ReactiveProduct> findAll() {
        return Flux.fromIterable(products);
    }

    /** Mono = 0 albo 1 element. Brak produktu = PUSTE Mono (nie null!). */
    public Mono<ReactiveProduct> findBySku(String sku) {
        return findAll().filter(product -> product.sku().equals(sku)).next();
    }
}
