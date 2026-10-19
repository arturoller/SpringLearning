package s18_aop.solutions.aop02_proxies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import s18_aop.aop02_proxies.CallCounter;
import s18_aop.aop02_proxies.CountingAspect;
import s18_aop.aop02_proxies.PriceCalculator;
import s18_aop.aop02_proxies.StandardPriceCalculator;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Aop02ProxiesExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
class Aop02ProxiesSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: rozpoznaj proxy JDK, proxy CGLIB i zwykły obiekt")
    void solution1() {
        StandardPriceCalculator plain = new StandardPriceCalculator();
        Object jdkProxy = new ProxyFactory(plain).getProxy();
        ProxyFactory cglibFactory = new ProxyFactory(plain);
        cglibFactory.setProxyTargetClass(true);
        Object cglibProxy = cglibFactory.getProxy();

        assertThat(Aop02ProxiesSolutions.solution1(jdkProxy)).isEqualTo("JDK");
        assertThat(Aop02ProxiesSolutions.solution1(cglibProxy)).isEqualTo("CGLIB");
        assertThat(Aop02ProxiesSolutions.solution1(plain)).isEqualTo("BRAK PROXY");
    }

    @Test
    @DisplayName("Wzorzec 2: reserveAll woła reserve przez proxy — licznik porady rośnie dla każdego SKU")
    void solution2() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
                .withBean(CallCounter.class)
                .withBean(CountingAspect.class)
                // nazwa kończy się na „Service”, żeby pasowała do bean(*Service) w CountingAspect
                .withBean("aop02Exercise2SolutionService", Aop02Exercise2ServiceSolution.class)
                .run(context -> {
                    Aop02Exercise2ServiceSolution service = context.getBean(Aop02Exercise2ServiceSolution.class);

                    List<String> results = service.reserveAll(List.of("SPO-001", "KSI-001", "ELE-003"));

                    assertThat(results).containsExactly(
                            "zarezerwowano 1 x SPO-001", "zarezerwowano 1 x KSI-001", "zarezerwowano 1 x ELE-003");
                    assertThat(context.getBean(CallCounter.class).count("reserve")).isEqualTo(3);
                });
    }

    @Test
    @DisplayName("Wzorzec 3: ręcznie zbudowane proxy JDK loguje wywołanie i nie jest instancją klasy")
    void solution3() {
        List<String> log = new ArrayList<>();

        PriceCalculator proxy = Aop02ProxiesSolutions.solution3(new StandardPriceCalculator(), log);

        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
        assertThat(proxy.priceFor("KSI-001", 2)).isEqualByComparingTo("158.00");
        assertThat(log).containsExactly("przed priceFor", "po priceFor");
        Object asObject = proxy;
        assertThatThrownBy(() -> ((StandardPriceCalculator) asObject).currency()).isInstanceOf(ClassCastException.class);
    }
}
