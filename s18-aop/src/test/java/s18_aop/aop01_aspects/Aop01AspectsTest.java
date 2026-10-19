package s18_aop.aop01_aspects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Aop01Aspects. {@code @SpringBootTest} startuje kontekst Aop01Application: aspekty działają tylko
 * w prawdziwym kontekście Springa (to Spring tworzy proxy), więc test jednostkowy bez Springa niczego by nie pokazał.
 * Dziennik (CallJournal) jest singletonem wspólnym dla testów, dlatego przed każdym testem go czyścimy.
 */
@SpringBootTest
class Aop01AspectsTest {

    @Autowired
    ProductCatalog catalog;

    @Autowired
    OrderService orderService;

    @Autowired
    StockReport stockReport;

    @Autowired
    CallJournal journal;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void clearJournal() {
        journal.clear();
    }

    @Test
    @DisplayName("1. Aspekt działa bez zmiany kodu katalogu: wstrzyknięty katalog to proxy, a dziennik nie")
    void aspectWorksThroughProxy() {
        assertThat(AopUtils.isAopProxy(catalog)).isTrue();
        // nazwa klasy proxy, np. ProductCatalog$$SpringCGLIB$$0 (numer na końcu może się różnić)
        assertThat(catalog.getClass().getName()).startsWith("s18_aop.aop01_aspects.ProductCatalog$$SpringCGLIB$$");
        assertThat(AopUtils.getTargetClass(catalog)).isEqualTo(ProductCatalog.class);
        assertThat(AopUtils.isAopProxy(journal)).as("dziennika nie dotyczy żaden punkt przecięcia").isFalse();

        catalog.findPrice("SPO-001");

        assertThat(journal.entriesStartingWith("LOG")).isNotEmpty();
    }

    @Test
    @DisplayName("2. @Aspect bez @Component nie jest beanem, więc jego porada się nie wykonuje")
    void aspectWithoutComponentIsIgnored() {
        assertThat(context.getBeanNamesForType(ForgottenAspect.class)).isEmpty();
        assertThat(context.getBeanNamesForType(LoggingAspect.class)).containsExactly("loggingAspect");

        catalog.findStock("SPO-001");

        assertThat(journal.entries()).contains("METODA findStock(SPO-001)");
        assertThat(journal.entries()).noneMatch(entry -> entry.startsWith("ZAPOMNIANY"));
    }

    @Test
    @DisplayName("3. Kolejność porad przy sukcesie: Around, Before, metoda, AfterReturning, After, Around")
    void adviceOrderOnSuccess() {
        BigDecimal price = catalog.findPrice("SPO-001");

        assertThat(price).isEqualByComparingTo("64.99");
        assertThat(logAndMethodEntries()).containsExactly(
                "LOG @Around: przed",
                "LOG @Before: findPrice [SPO-001]",
                "METODA findPrice(SPO-001)",
                "LOG @AfterReturning: wynik 64.99",
                "LOG @After: findPrice zakończona",
                "LOG @Around: po, wynik 64.99");
    }

    @Test
    @DisplayName("3. Kolejność porad przy wyjątku: AfterThrowing zamiast AfterReturning, After biegnie zawsze")
    void adviceOrderOnException() {
        assertThatThrownBy(() -> catalog.findPrice("XYZ-999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nieznany SKU: XYZ-999");

        assertThat(logAndMethodEntries()).containsExactly(
                "LOG @Around: przed",
                "LOG @Before: findPrice [XYZ-999]",
                "METODA findPrice(XYZ-999)",
                "LOG @AfterThrowing: Nieznany SKU: XYZ-999",
                "LOG @After: findPrice zakończona",
                "LOG @Around: wyjątek IllegalArgumentException");
    }

    @Test
    @DisplayName("4. @Around może podmienić argumenty metody: proceed(noweArgumenty)")
    void aroundCanReplaceArguments() {
        String result = orderService.placeOrder(" spo-001 ", 2);

        assertThat(result).isEqualTo("Zamówienie przyjęte: 2 x SPO-001");
        assertThat(journal.entries()).contains("NORM ' spo-001 ' -> 'SPO-001'", "METODA placeOrder(SPO-001, 2)");
    }

    @Test
    @DisplayName("4. PUŁAPKA: @Around bez proceed() — metoda się nie wykonuje; null dla obiektu, wyjątek dla int")
    void aroundWithoutProceed() {
        assertThat(stockReport.title()).isNull();
        assertThatThrownBy(() -> stockReport.productCount())
                .isInstanceOf(AopInvocationException.class)
                .hasMessage("Null return value from advice does not match primitive return type for: "
                        + "public int s18_aop.aop01_aspects.StockReport.productCount()");

        assertThat(journal.entries()).as("ciało metod StockReport ani razu się nie wykonało").containsExactly(
                "ZEPSUTY @Around: title bez proceed()",
                "ZEPSUTY @Around: productCount bez proceed()");
    }

    @Test
    @DisplayName("5. Wyrażenia execution, args, within, bean i @annotation łapią różne metody")
    void pointcutExpressions() {
        catalog.findPrice("SPO-001");
        assertThat(pointcutEntries()).containsExactlyInAnyOrder(
                "PC execution(find*): findPrice",
                "PC args(sku): findPrice sku=SPO-001");

        journal.clear();
        catalog.findCheaperThan(new BigDecimal("50"));
        assertThat(pointcutEntries()).as("args(sku) wymaga jednego argumentu typu String")
                .containsExactly("PC execution(find*): findCheaperThan");

        journal.clear();
        orderService.placeOrder("SPO-001", 1);
        assertThat(pointcutEntries()).as("placeOrder woła katalog przez proxy, więc findStock też jest złapane")
                .containsExactlyInAnyOrder(
                        "PC within(OrderService): placeOrder",
                        "PC bean(orderService): placeOrder",
                        "PC execution(find*): findStock",
                        "PC args(sku): findStock sku=SPO-001");
    }

    @Test
    @DisplayName("5. @annotation z pełną nazwą działa, a krótka nazwa z importu po cichu nie łapie niczego")
    void annotationPointcutNeedsFullName() {
        BigDecimal value = catalog.stockValue();

        assertThat(value).isEqualByComparingTo("49730.73");
        assertThat(pointcutEntries()).containsExactly("PC @annotation(RolesAllowed): stockValue");
        assertThat(journal.entries()).noneMatch(entry -> entry.startsWith("PC PUŁAPKA import"));
    }

    @Test
    @DisplayName("5. PUŁAPKA: za szeroki punkt przecięcia łapie beany Spring Boota — aplikacja nie startuje")
    void tooBroadPointcutBreaksStartup() {
        // Prawdziwa aplikacja lekcji + jeden dodatkowy aspekt bez zawężenia do naszych pakietów.
        SpringApplicationBuilder application = new SpringApplicationBuilder(Aop01Application.class, BroadDeprecatedAspect.class)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false);

        assertThatThrownBy(application::run)
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Could not generate CGLIB subclass of class "
                        + "org.springframework.boot.autoconfigure.AutoConfigurationPackages$BasePackages")
                .hasRootCauseMessage("Cannot subclass final class "
                        + "org.springframework.boot.autoconfigure.AutoConfigurationPackages$BasePackages");
    }

    /** Wpisy porad LoggingAspect i ciała metody — bez wpisów innych aspektów (ich kolejność to osobny temat). */
    private List<String> logAndMethodEntries() {
        return journal.entries().stream()
                .filter(entry -> entry.startsWith("LOG") || entry.startsWith("METODA"))
                .toList();
    }

    private List<String> pointcutEntries() {
        return journal.entriesStartingWith("PC");
    }
}
