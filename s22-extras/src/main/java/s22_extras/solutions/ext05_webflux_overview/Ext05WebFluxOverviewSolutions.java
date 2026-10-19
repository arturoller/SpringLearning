package s22_extras.solutions.ext05_webflux_overview;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import s22_extras.ext05_webflux_overview.Ext05WebFluxOverview.OrderLine;
import s22_extras.ext05_webflux_overview.ReactiveProduct;

import java.math.BigDecimal;
import java.util.Locale;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ext05WebFluxOverview — zajrzyj dopiero po własnej próbie! */
public final class Ext05WebFluxOverviewSolutions {

    private Ext05WebFluxOverviewSolutions() {
    }

    static Flux<String> solution1(Flux<ReactiveProduct> products) {
        return products
                .filter(product -> product.stock() > 0)
                .map(product -> product.name().toUpperCase(Locale.ROOT));
    }

    static Mono<String> solution2(Mono<ReactiveProduct> product) {
        return product
                .map(p -> p.name() + ": " + p.price().toPlainString() + " zł")
                .defaultIfEmpty("brak produktu")          // puste Mono → wartość domyślna
                .onErrorReturn("cena niedostępna");        // błąd (na dowolnym etapie wyżej) → wartość zastępcza
    }

    static Mono<BigDecimal> solution3(Flux<OrderLine> lines) {
        return lines
                .filter(line -> line.quantity() > 0)
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);  // z wartością początkową: pusty Flux → ZERO
    }
}
