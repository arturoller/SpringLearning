package s18_aop.aop02_proxies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Aop02Proxies w trybie proxy JDK: {@code spring.aop.proxy-target-class=false} ustawione TYLKO tutaj
 * (properties w {@code @SpringBootTest}), więc nie zmienia zachowania pozostałych testów.
 * Uwaga: wstrzykujemy PriceCalculator (interfejs) — wstrzyknięcie StandardPriceCalculator (klasy) w tym trybie
 * wywróciłoby cały kontekst testu (to właśnie pokazuje ostatni test).
 */
@SpringBootTest(properties = "spring.aop.proxy-target-class=false")
class Aop02ProxiesJdkTest {

    @Autowired
    PriceCalculator priceCalculator;

    @Autowired
    StockService stockService;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("3. Przy proxy-target-class=false klasa z interfejsem dostaje proxy JDK, a klasa bez interfejsu — CGLIB")
    void jdkProxyForInterfacesCglibForClasses() {
        assertThat(AopUtils.isJdkDynamicProxy(priceCalculator)).isTrue();
        assertThat(priceCalculator.getClass().getName()).startsWith("jdk.proxy");
        assertThat(priceCalculator).isNotInstanceOf(StandardPriceCalculator.class);
        assertThat(AopUtils.getTargetClass(priceCalculator)).isEqualTo(StandardPriceCalculator.class);

        assertThat(AopUtils.isCglibProxy(stockService)).as("StockService nie ma interfejsu — zostaje CGLIB").isTrue();
    }

    @Test
    @DisplayName("3. Proxy JDK nie jest instancją klasy: getBean(klasa) i rzutowanie kończą się wyjątkami")
    void jdkProxyIsNotTheClass() {
        assertThatThrownBy(() -> context.getBean(StandardPriceCalculator.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class)
                .hasMessage("No qualifying bean of type 's18_aop.aop02_proxies.StandardPriceCalculator' available");

        assertThatThrownBy(() -> context.getBean("standardPriceCalculator", StandardPriceCalculator.class))
                .isInstanceOf(BeanNotOfRequiredTypeException.class)
                .hasMessageStartingWith("Bean named 'standardPriceCalculator' is expected to be of type "
                        + "'s18_aop.aop02_proxies.StandardPriceCalculator' but was actually of type 'jdk.proxy");

        Object bean = context.getBean("standardPriceCalculator");
        assertThatThrownBy(() -> ((StandardPriceCalculator) bean).currency())
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("cannot be cast to class s18_aop.aop02_proxies.StandardPriceCalculator");
    }

    @Test
    @DisplayName("3. Wstrzyknięcie przez klasę zamiast interfejsu: przy proxy JDK aplikacja nie startuje")
    @ExtendWith(OutputCaptureExtension.class)
    void injectingClassFailsWithJdkProxy(CapturedOutput output) {
        // Prawdziwa aplikacja lekcji + bean CheckoutByClass, który chce StandardPriceCalculator przez KLASĘ.
        SpringApplicationBuilder application = new SpringApplicationBuilder(Aop02Application.class, CheckoutByClass.class)
                .properties("spring.aop.proxy-target-class=false")
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false);

        assertThatThrownBy(application::run)
                .isInstanceOf(UnsatisfiedDependencyException.class)
                .hasRootCauseInstanceOf(BeanNotOfRequiredTypeException.class);

        // Raport Spring Boota (FailureAnalyzer — analizator błędów startu) w logu:
        assertThat(output).contains(
                "The bean 'standardPriceCalculator' could not be injected because it is a JDK dynamic proxy",
                "Consider injecting the bean as one of its interfaces or forcing the use of CGLib-based proxies");
    }
}
