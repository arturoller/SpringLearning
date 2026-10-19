package s18_aop.aop01_aspects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * Testy ĆWICZEŃ lekcji Aop01Aspects. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Bez kontekstu Springa: AspectJProxyFactory (fabryka proxy z aspektami AspectJ) zakłada aspekt ćwiczenia na zwykły
 * obiekt katalogu. To ten sam mechanizm porad, którego używa Spring, tylko uruchomiony ręcznie i w izolacji.
 */
@Tag("cwiczenie")
class Aop01AspectsExercisesTest {

    /** Katalog opakowany w proxy z JEDNYM aspektem (instancją podaną przez test). */
    static ProductCatalog catalogWith(Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new ProductCatalog(new CallJournal()));
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    @DisplayName("Ćwiczenie 1: @Before zapisuje nazwę i argumenty każdej metody find*")
    void exercise1() {
        List<String> calls = new ArrayList<>();
        ProductCatalog catalog = catalogWith(new Aop01Exercise1Aspect(calls));

        catalog.findPrice("SPO-001");
        catalog.findStock("KSI-001");
        catalog.stockValue(); // nie zaczyna się od „find” — nie powinno być wpisu

        assertThat(calls).containsExactly("wywołanie: findPrice[SPO-001]", "wywołanie: findStock[KSI-001]");
    }

    @Test
    @DisplayName("Ćwiczenie 2: wyrażenie łapie tylko metody find* katalogu z jednym argumentem String")
    void exercise2() throws Exception {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(Aop01Aspects.exercise2Pointcut());

        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findPrice", String.class), ProductCatalog.class)).isTrue();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findStock", String.class), ProductCatalog.class)).isTrue();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findName", String.class), ProductCatalog.class)).isTrue();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findCheaperThan", BigDecimal.class), ProductCatalog.class))
                .as("parametr BigDecimal, nie String").isFalse();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("stockValue"), ProductCatalog.class))
                .as("nazwa nie zaczyna się od find").isFalse();
        assertThat(pointcut.matches(OrderService.class.getMethod("placeOrder", String.class, int.class), OrderService.class))
                .as("inna klasa").isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: @Around liczy wywołania i zwraca prawdziwe wyniki metod")
    void exercise3() {
        Aop01Exercise3Aspect aspect = new Aop01Exercise3Aspect();
        ProductCatalog catalog = catalogWith(aspect);

        assertThat(catalog.findPrice("SPO-001")).isEqualByComparingTo("64.99");
        assertThat(catalog.findPrice("KSI-001")).isEqualByComparingTo("79.00");
        assertThat(catalog.findStock("ELE-001")).isEqualTo(7);

        assertThat(aspect.counts()).containsExactly(entry("findPrice", 2), entry("findStock", 1));
    }

    @Test
    @DisplayName("Ćwiczenie 4: @AfterThrowing zamienia IllegalArgumentException na ProductNotFoundException")
    void exercise4() {
        ProductCatalog catalog = catalogWith(new Aop01Exercise4Aspect());

        assertThat(catalog.findPrice("SPO-001")).isEqualByComparingTo("64.99");
        assertThatThrownBy(() -> catalog.findPrice("XYZ-999"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Brak produktu w katalogu (Nieznany SKU: XYZ-999)")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
