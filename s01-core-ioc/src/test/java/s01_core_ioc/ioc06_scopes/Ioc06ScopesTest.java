package s01_core_ioc.ioc06_scopes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Ioc06Scopes. {@code @SpringBootTest} startuje kontekst Ioc06Application (potrzebujemy prawdziwych
 * beanów z wstrzykiwaniem); sekcja 2b używa małego ApplicationContextRunner (runner = uruchamiacz małych kontekstów).
 * Uwaga: kontekst jest wspólny dla testów w klasie, więc singletony mają stan z poprzednich testów —
 * dlatego porównujemy przyrosty, a nie wartości bezwzględne.
 */
@SpringBootTest
class Ioc06ScopesTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    BrokenCartService brokenCartService;

    @Autowired
    CartService cartService;

    @Autowired
    HomePageService homePageService;

    @Autowired
    ReportService reportService;

    @Autowired
    VisitCounter visitCounter;

    @Autowired
    SafeVisitCounter safeVisitCounter;

    @Test
    @DisplayName("1. Singleton jest domyślny: getBean zwraca ten sam obiekt, a oba serwisy dzielą jeden licznik")
    void singletonIsDefault() {
        assertThat(context.getBean(VisitCounter.class)).isSameAs(context.getBean(VisitCounter.class));
        assertThat(context.getBean(CartService.class)).isSameAs(cartService);
        assertThat(context.isSingleton("visitCounter")).isTrue();
        assertThat(context.isPrototype("visitCounter")).isFalse();
    }

    @Test
    @DisplayName("2. Prototype: każde getBean daje nowy koszyk")
    void prototypeCreatesNewObjects() {
        ShoppingCart first = context.getBean(ShoppingCart.class);
        ShoppingCart second = context.getBean(ShoppingCart.class);

        assertThat(context.isPrototype("shoppingCart")).isTrue();
        assertThat(first).isNotSameAs(second);
        first.add("Kawa");
        assertThat(second.size()).isZero();
    }

    @Test
    @DisplayName("2b. Metoda @Bean bez @Scope jest singletonem, mimo @Scope(prototype) na klasie koszyka")
    void beanMethodDefaultsToSingleton() {
        new ApplicationContextRunner()
                .withUserConfiguration(Ioc06Exercise3Config.class)
                .run(ctx -> assertThat(ctx.getBean(ShoppingCart.class)).isSameAs(ctx.getBean(ShoppingCart.class)));
    }

    @Test
    @DisplayName("3. Prototype w singletonie wstrzyknięty raz: zepsuty serwis zlewa koszyki, poprawny daje nowy")
    void prototypeInSingletonIsInjectedOnlyOnce() {
        // zepsuty: ten sam koszyk, więc drugi klient widzi produkt pierwszego
        int first = brokenCartService.addAndCount("Kawa");
        int second = brokenCartService.addAndCount("Herbata");
        assertThat(second).isEqualTo(first + 1);
        assertThat(brokenCartService.cart()).isSameAs(brokenCartService.cart());
        assertThat(brokenCartService.cart()).isNotSameAs(context.getBean(ShoppingCart.class));

        // poprawny: ObjectProvider daje nowy koszyk za każdym razem
        assertThat(cartService.addAndCount("Kawa")).isEqualTo(1);
        assertThat(cartService.addAndCount("Herbata")).isEqualTo(1);
        assertThat(cartService.newCart()).isNotSameAs(cartService.newCart());
    }

    @Test
    @DisplayName("4. Stan singletona jest wspólny: odwiedziny ze strony głównej widać w raporcie")
    void singletonStateIsShared() {
        int before = reportService.totalVisits();
        homePageService.open();
        homePageService.open();
        assertThat(reportService.totalVisits()).isEqualTo(before + 2);
        assertThat(visitCounter.count()).isEqualTo(reportService.totalVisits());
    }

    @Test
    @DisplayName("5. Licznik oparty o AtomicInteger nie gubi zliczeń przy wielu wątkach")
    void atomicCounterSurvivesManyThreads() throws Exception {
        int before = safeVisitCounter.count();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < 4; thread++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < 1000; i++) {
                        safeVisitCounter.increment();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdown();
        }
        assertThat(safeVisitCounter.count()).isEqualTo(before + 4000);
    }
}
