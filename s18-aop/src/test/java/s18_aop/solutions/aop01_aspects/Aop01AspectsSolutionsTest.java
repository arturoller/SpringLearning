package s18_aop.solutions.aop01_aspects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import s18_aop.aop01_aspects.CallJournal;
import s18_aop.aop01_aspects.OrderService;
import s18_aop.aop01_aspects.ProductCatalog;
import s18_aop.aop01_aspects.ProductNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/** Sprawdzenia z Aop01AspectsExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
class Aop01AspectsSolutionsTest {

    static ProductCatalog catalogWith(Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new ProductCatalog(new CallJournal()));
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    @DisplayName("Wzorzec 1: @Before zapisuje nazwę i argumenty każdej metody find*")
    void solution1() {
        List<String> calls = new ArrayList<>();
        ProductCatalog catalog = catalogWith(new Aop01Exercise1AspectSolution(calls));

        catalog.findPrice("SPO-001");
        catalog.findStock("KSI-001");
        catalog.stockValue();

        assertThat(calls).containsExactly("wywołanie: findPrice[SPO-001]", "wywołanie: findStock[KSI-001]");
    }

    @Test
    @DisplayName("Wzorzec 2: wyrażenie łapie tylko metody find* katalogu z jednym argumentem String")
    void solution2() throws Exception {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(Aop01AspectsSolutions.solution2Pointcut());

        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findPrice", String.class), ProductCatalog.class)).isTrue();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findStock", String.class), ProductCatalog.class)).isTrue();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findName", String.class), ProductCatalog.class)).isTrue();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("findCheaperThan", BigDecimal.class), ProductCatalog.class)).isFalse();
        assertThat(pointcut.matches(ProductCatalog.class.getMethod("stockValue"), ProductCatalog.class)).isFalse();
        assertThat(pointcut.matches(OrderService.class.getMethod("placeOrder", String.class, int.class), OrderService.class)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: @Around liczy wywołania i zwraca prawdziwe wyniki metod")
    void solution3() {
        Aop01Exercise3AspectSolution aspect = new Aop01Exercise3AspectSolution();
        ProductCatalog catalog = catalogWith(aspect);

        assertThat(catalog.findPrice("SPO-001")).isEqualByComparingTo("64.99");
        assertThat(catalog.findPrice("KSI-001")).isEqualByComparingTo("79.00");
        assertThat(catalog.findStock("ELE-001")).isEqualTo(7);

        assertThat(aspect.counts()).containsExactly(entry("findPrice", 2), entry("findStock", 1));
    }

    @Test
    @DisplayName("Wzorzec 4: @AfterThrowing zamienia IllegalArgumentException na ProductNotFoundException")
    void solution4() {
        ProductCatalog catalog = catalogWith(new Aop01Exercise4AspectSolution());

        assertThat(catalog.findPrice("SPO-001")).isEqualByComparingTo("64.99");
        assertThatThrownBy(() -> catalog.findPrice("XYZ-999"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Brak produktu w katalogu (Nieznany SKU: XYZ-999)")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
