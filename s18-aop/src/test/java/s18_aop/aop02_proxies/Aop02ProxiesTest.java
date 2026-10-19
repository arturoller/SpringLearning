package s18_aop.aop02_proxies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Aop02Proxies przy USTAWIENIACH DOMYŚLNYCH Spring Boota (proxy CGLIB).
 * Tryb proxy JDK (spring.aop.proxy-target-class=false) ma osobną klasę testów: Aop02ProxiesJdkTest.
 * {@code @SpringBootTest}, bo proxy tworzy kontener Springa — bez kontekstu nie ma czego oglądać.
 * Test leży w tym samym pakiecie co WarehouseService, więc może wołać jej metody protected i pakietowe.
 */
@SpringBootTest
class Aop02ProxiesTest {

    @Autowired
    CallCounter counter;

    @Autowired
    StockService stockService;

    @Autowired
    PriceCalculator priceCalculator;

    @Autowired
    WarehouseService warehouseService;

    @Autowired
    ReportPrinter reportPrinter;

    @Autowired
    DiscountPolicy discountPolicy;

    @Autowired
    StockBatchService stockBatchService;

    @Autowired
    SelfInjectingStockService selfInjectingStockService;

    @Autowired
    AopContextStockService aopContextStockService;

    /** Mały kontekst z samą autokonfiguracją AOP Boota, licznikiem i aspektem — do testów „czy w ogóle wstanie”. */
    private final ApplicationContextRunner aopRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
            .withBean(CallCounter.class)
            .withBean(CountingAspect.class);

    @BeforeEach
    void resetCounter() {
        counter.reset();
    }

    @Test
    @DisplayName("1. Proxy dostaje tylko bean, którego dotyczy punkt przecięcia; w środku proxy siedzi zwykły obiekt")
    void proxyOnlyForMatchedBeans() throws Exception {
        assertThat(AopUtils.isAopProxy(stockService)).isTrue();
        assertThat(stockService.getClass().getName()).startsWith("s18_aop.aop02_proxies.StockService$$SpringCGLIB$$");
        assertThat(AopUtils.getTargetClass(stockService)).isEqualTo(StockService.class);

        // Advised (doradzany) — interfejs, który ma każde proxy Springa: lista porad i obiekt docelowy
        Advised advised = (Advised) stockService;
        assertThat(Arrays.toString(advised.getAdvisors())).contains("CountingAspect.count");
        Object target = advised.getTargetSource().getTarget();
        assertThat(target).isExactlyInstanceOf(StockService.class);
        assertThat(AopUtils.isAopProxy(target)).isFalse();

        assertThat(AopUtils.isAopProxy(reportPrinter)).as("nazwa reportPrinter nie pasuje do bean(*Service)").isFalse();
        assertThat(reportPrinter.getClass()).isEqualTo(ReportPrinter.class);
    }

    @Test
    @DisplayName("2. Boot domyślnie robi proxy CGLIB — także dla klasy z interfejsem")
    void bootUsesCglibByDefault() {
        assertThat(AopUtils.isCglibProxy(priceCalculator)).isTrue();
        assertThat(AopUtils.isJdkDynamicProxy(priceCalculator)).isFalse();
        assertThat(priceCalculator).isInstanceOf(StandardPriceCalculator.class);
        // proxy jest podklasą, więc rzutowanie na klasę działa i widać metodę spoza interfejsu
        assertThat(((StandardPriceCalculator) priceCalculator).currency()).isEqualTo("PLN");
    }

    @Test
    @DisplayName("2. Czysty Spring (@EnableAspectJAutoProxy bez Boota) robi dla klasy z interfejsem proxy JDK")
    void plainSpringUsesJdkProxyForInterfaces() {
        try (AnnotationConfigApplicationContext plain = new AnnotationConfigApplicationContext(PlainSpringConfig.class)) {
            Object calculator = plain.getBean("standardPriceCalculator");

            assertThat(AopUtils.isJdkDynamicProxy(calculator)).isTrue();
            assertThat(calculator.getClass().getName()).startsWith("jdk.proxy");
            assertThat(calculator).isInstanceOf(PriceCalculator.class).isNotInstanceOf(StandardPriceCalculator.class);
        }
    }

    @Test
    @DisplayName("2. @Proxyable(INTERFACES) wybiera proxy JDK dla jednego beana — nawet dla klasy final")
    void proxyableChoosesJdkProxyForOneBean() {
        assertThat(AopUtils.isJdkDynamicProxy(discountPolicy)).isTrue();
        assertThat(AopUtils.getTargetClass(discountPolicy)).isEqualTo(VipDiscountCalculator.class);
        assertThat(discountPolicy.discountPercent("Jan Kowalski")).isEqualByComparingTo("10");
        assertThat(discountPolicy.discountPercent("Maria Nowak")).isEqualByComparingTo("0");
        assertThat(counter.count("discountPercent")).isEqualTo(2);
    }

    @Test
    @DisplayName("4. this.metoda() omija proxy: reserveAll nie przechodzi przez poradę dla reserve")
    void selfInvocationBypassesProxy() {
        stockService.reserve("SPO-001", 2);
        assertThat(counter.count("reserve")).as("wywołanie z zewnątrz — przez proxy").isEqualTo(1);

        List<String> results = stockService.reserveAll(List.of("SPO-001", "KSI-001", "ELE-003"));

        assertThat(results).hasSize(3);
        assertThat(counter.count("reserveAll")).isEqualTo(1);
        assertThat(counter.count("reserve")).as("trzy wywołania this.reserve — żadne nie przeszło przez proxy").isEqualTo(1);
        assertThat(AopUtils.isAopProxy(stockService)).isTrue();
        assertThat(stockService.thisIsProxy()).as("wewnątrz metody this to obiekt docelowy, nie proxy").isFalse();
    }

    @Test
    @DisplayName("5. CGLIB przechwytuje metody public, protected i pakietowe; private i final — nie")
    void whichMethodsCglibCanIntercept() {
        assertThat(warehouseService.stockOf("SPO-001")).isEqualTo(120);
        assertThat(warehouseService.protectedInfo()).isEqualTo("protected");
        assertThat(warehouseService.packageInfo()).isEqualTo("pakietowa");
        assertThat(warehouseService.publicCallingPrivate()).isEqualTo("public → private");

        assertThat(counter.count("stockOf")).isEqualTo(1);
        assertThat(counter.count("protectedInfo")).isEqualTo(1);
        assertThat(counter.count("packageInfo")).isEqualTo(1);
        assertThat(counter.count("publicCallingPrivate")).isEqualTo(1);
        assertThat(counter.count("privateInfo")).as("metoda private — wywołana wewnątrz obiektu").isZero();
    }

    @Test
    @DisplayName("5. Metoda final: proxy jej nie przechwytuje i wykonuje ją na sobie — z pustymi polami (NPE)")
    void finalMethodRunsOnProxyWithEmptyFields() {
        assertThatThrownBy(() -> warehouseService.finalStockOf("SPO-001"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Cannot invoke \"java.util.Map.getOrDefault(Object, Object)\" because \"this.stock\" is null");

        assertThat(counter.count("finalStockOf")).isZero();
    }

    @Test
    @DisplayName("5. Klasa final z punktem przecięcia: przy CGLIB kontekst nie startuje, przy proxy JDK — startuje")
    void finalClassCannotBeSubclassed() {
        aopRunner.withBean("finalPriceService", FinalPriceService.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("Could not generate CGLIB subclass of class "
                                    + "s18_aop.aop02_proxies.FinalPriceService")
                            .hasRootCauseMessage("Cannot subclass final class s18_aop.aop02_proxies.FinalPriceService");
                });

        aopRunner.withBean("finalPriceService", FinalPriceService.class)
                .withPropertyValues("spring.aop.proxy-target-class=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    PriceCalculator calculator = context.getBean(PriceCalculator.class);
                    assertThat(AopUtils.isJdkDynamicProxy(calculator)).isTrue();
                    assertThat(calculator.priceFor("SPO-002", 2)).isEqualByComparingTo("14.98");
                });
    }

    @Test
    @DisplayName("6. Obejścia: osobny bean, wstrzyknięty sam siebie (@Lazy) i AopContext — każde reserve przez proxy")
    void workaroundsGoThroughProxy() {
        List<String> skus = List.of("SPO-001", "KSI-001", "ELE-003");

        assertThat(stockBatchService.reserveAll(skus)).hasSize(3);
        assertThat(counter.count("reserve")).as("osobny bean").isEqualTo(3);

        counter.reset();
        assertThat(selfInjectingStockService.reserveAll(skus)).hasSize(3);
        assertThat(counter.count("reserve")).as("@Lazy self").isEqualTo(3);

        counter.reset();
        assertThat(aopContextStockService.reserveAll(skus)).hasSize(3);
        assertThat(counter.count("reserve")).as("AopContext.currentProxy()").isEqualTo(3);
    }

    @Test
    @DisplayName("6. AopContext.currentProxy() poza wywołaniem przez proxy rzuca IllegalStateException")
    void aopContextOutsideProxyCall() {
        assertThatThrownBy(AopContext::currentProxy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it "
                        + "available, and ensure that AopContext.currentProxy() is invoked in the same thread as the "
                        + "AOP invocation context.");
    }

    @Test
    @DisplayName("6. Bez exposeProxy = true AopContext.currentProxy() nie działa nawet wewnątrz wywołania przez proxy")
    void aopContextWithoutExposeProxy() {
        aopRunner.withBean("aopContextStockService", AopContextStockService.class)
                .run(context -> {
                    AopContextStockService service = context.getBean(AopContextStockService.class);
                    assertThat(AopUtils.isCglibProxy(service)).isTrue();
                    assertThatThrownBy(() -> service.reserveAll(List.of("SPO-001")))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageStartingWith("Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true'");
                });
    }
}
