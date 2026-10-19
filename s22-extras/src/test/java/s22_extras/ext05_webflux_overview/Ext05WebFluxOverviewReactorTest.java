package s22_extras.ext05_webflux_overview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Ext05WebFluxOverview, sekcja 1: Mono i Flux bez Springa. StepVerifier (z reactor-test) subskrybuje
 * strumień i sprawdza krok po kroku, co z niego wypływa.
 */
class Ext05WebFluxOverviewReactorTest {

    final ReactiveCatalog catalog = new ReactiveCatalog();

    @Test
    @DisplayName("1. Nic się nie dzieje bez subskrypcji: fromSupplier czeka, a każda subskrypcja liczy OD NOWA")
    void lazyUntilSubscribe() {
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> mono = Mono.fromSupplier(calls::incrementAndGet);   // tylko PRZEPIS

        assertThat(calls).hasValue(0);                                   // nikt nie subskrybował — zero wywołań
        StepVerifier.create(mono).expectNext(1).verifyComplete();
        StepVerifier.create(mono).expectNext(2).verifyComplete();      // druga subskrypcja = drugie wywołanie
        assertThat(calls).hasValue(2);
    }

    @Test
    @DisplayName("1. PUŁAPKA: Mono.just(wyrażenie) liczy wyrażenie OD RAZU — jeszcze przed subskrypcją")
    void justIsEager() {
        AtomicInteger calls = new AtomicInteger();

        Mono<Integer> mono = Mono.just(calls.incrementAndGet());         // incrementAndGet() wykonane TERAZ

        assertThat(calls).hasValue(1);
        StepVerifier.create(mono).expectNext(1).verifyComplete();
        StepVerifier.create(mono).expectNext(1).verifyComplete();      // ta sama, gotowa wartość
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("1. Flux: filter + map jak w Stream API, StepVerifier sprawdza kolejne elementy i zakończenie")
    void fluxOperators() {
        Flux<String> inStock = catalog.findAll()
                .filter(product -> product.stock() > 0)
                .map(ReactiveProduct::name);

        StepVerifier.create(inStock)
                .expectNext("Laptop Pro 14", "Słuchawki BT", "Kawa ziarnista 1kg")
                .verifyComplete();
    }

    @Test
    @DisplayName("1. Błąd kończy strumień (dalszych elementów nie ma); onErrorReturn podstawia wartość zastępczą")
    void errorsEndTheStream() {
        Flux<Integer> division = Flux.just(1, 0, 2).map(x -> 10 / x);

        StepVerifier.create(division).expectNext(10).expectError(ArithmeticException.class).verify();
        StepVerifier.create(division.onErrorReturn(-1)).expectNext(10, -1).verifyComplete();
    }

    @Test
    @DisplayName("1. Puste Mono to nie null: findBySku dla nieznanego SKU kończy się bez elementu")
    void emptyMono() {
        StepVerifier.create(catalog.findBySku("XXX-999")).verifyComplete();
        StepVerifier.create(catalog.findBySku("XXX-999").defaultIfEmpty(
                new ReactiveProduct("BRAK", "brak", java.math.BigDecimal.ZERO, 0)))
                .expectNextMatches(product -> product.sku().equals("BRAK"))
                .verifyComplete();
    }
}
