package s22_extras.ext05_webflux_overview;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Kontroler WebFlux (lekcja Ext05WebFluxOverview). Te same adnotacje co w Spring MVC (@RestController, @GetMapping),
 * ale metody zwracają Mono/Flux — WebFlux sam subskrybuje wynik i zapisuje odpowiedź, gdy dane będą gotowe.
 */
@RestController
@RequestMapping("/api/reactive/products")
public class ReactiveProductController {

    /** Cena hurtowa i nazwa wątku, na którym ją policzono (do pokazania przełączenia wątku). */
    public record WholesalePrice(String sku, BigDecimal price, String thread) {
    }

    private final ReactiveCatalog catalog;
    private final LegacyPriceService legacyPriceService;

    public ReactiveProductController(ReactiveCatalog catalog, LegacyPriceService legacyPriceService) {
        this.catalog = catalog;
        this.legacyPriceService = legacyPriceService;
    }

    /** Sekcja 3: Flux → tablica JSON. */
    @GetMapping
    public Flux<ReactiveProduct> all() {
        return catalog.findAll();
    }

    /** Sekcja 3: Mono → obiekt JSON; puste Mono → 404 (switchIfEmpty = „jeśli pusto, to...”). */
    @GetMapping("/{sku}")
    public Mono<ReactiveProduct> one(@PathVariable String sku) {
        return catalog.findBySku(sku)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Brak produktu " + sku)));
    }

    /**
     * Sekcja 4 — ŹLE: block() w środku potoku reaktywnego. block() = „zatrzymaj wątek i czekaj na wynik”. Na wątku
     * pętli zdarzeń Netty (reactor-http-nio-N) Reactor tego zabrania i rzuca IllegalStateException.
     */
    @GetMapping("/{sku}/blocking-stock")
    public Mono<Integer> blockingStock(@PathVariable String sku) {
        Integer stock = catalog.findBySku(sku)
                .delayElement(Duration.ofMillis(10))   // „wolne źródło” — wynik przyjdzie na innym wątku
                .map(ReactiveProduct::stock)
                .block();                               // ŹLE: blokuje wątek, który obsługuje zapytanie
        return Mono.justOrEmpty(stock);
    }

    /**
     * Sekcja 4 — DOBRZE: blokujący kod opakowany w Mono.fromCallable i przeniesiony na pulę wątków do blokowania
     * (Schedulers.boundedElastic()). Wątek pętli zdarzeń zostaje wolny.
     */
    @GetMapping("/{sku}/wholesale-price")
    public Mono<WholesalePrice> wholesalePrice(@PathVariable String sku) {
        return catalog.findBySku(sku)
                .flatMap(product -> Mono.fromCallable(() -> new WholesalePrice(sku,
                                legacyPriceService.wholesalePrice(product), Thread.currentThread().getName()))
                        .subscribeOn(Schedulers.boundedElastic()))   // subscribeOn = „wykonaj na tej puli”
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Brak produktu " + sku)));
    }
}
