package s22_extras.solutions.ext05_webflux_overview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import s22_extras.ext05_webflux_overview.Ext05WebFluxOverview.OrderLine;
import s22_extras.ext05_webflux_overview.ReactiveCatalog;

import java.math.BigDecimal;

/** Sprawdzenia z Ext05WebFluxOverviewExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ext05WebFluxOverviewSolutionsTest {

    final ReactiveCatalog catalog = new ReactiveCatalog();

    @Test
    @DisplayName("Wzorzec 1: nazwy dostępnych produktów WIELKIMI literami")
    void solution1() {
        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution1(catalog.findAll()))
                .expectNext("LAPTOP PRO 14", "SŁUCHAWKI BT", "KAWA ZIARNISTA 1KG")
                .verifyComplete();
        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution1(Flux.empty())).verifyComplete();
    }

    @Test
    @DisplayName("Wzorzec 2: etykieta ceny, puste Mono i błąd")
    void solution2() {
        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution2(catalog.findBySku("ELE-003")))
                .expectNext("Słuchawki BT: 349.90 zł").verifyComplete();
        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution2(catalog.findBySku("XXX-999")))
                .expectNext("brak produktu").verifyComplete();
        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution2(Mono.error(new IllegalStateException("baza nie działa"))))
                .expectNext("cena niedostępna").verifyComplete();
    }

    @Test
    @DisplayName("Wzorzec 3: wartość zamówienia przez reduce (pozycje z 0 sztuk pomijane)")
    void solution3() {
        Flux<OrderLine> lines = Flux.just(
                new OrderLine("SPO-001", 2, new BigDecimal("64.99")),
                new OrderLine("ELE-003", 1, new BigDecimal("349.90")),
                new OrderLine("KSI-001", 0, new BigDecimal("79.00")));

        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution3(lines))
                .expectNextMatches(total -> total.compareTo(new BigDecimal("479.88")) == 0)
                .verifyComplete();
        StepVerifier.create(Ext05WebFluxOverviewSolutions.solution3(Flux.empty()))
                .expectNextMatches(total -> total.compareTo(BigDecimal.ZERO) == 0)
                .verifyComplete();
    }
}
