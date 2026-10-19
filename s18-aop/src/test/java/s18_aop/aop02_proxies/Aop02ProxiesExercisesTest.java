package s18_aop.aop02_proxies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Aop02Proxies. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Aop02ProxiesExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: rozpoznaj proxy JDK, proxy CGLIB i zwykły obiekt")
    void exercise1() {
        StandardPriceCalculator plain = new StandardPriceCalculator();
        Object jdkProxy = new ProxyFactory(plain).getProxy(); // klasa z interfejsem: domyślnie proxy JDK
        ProxyFactory cglibFactory = new ProxyFactory(plain);
        cglibFactory.setProxyTargetClass(true);               // wymuś podklasę (CGLIB)
        Object cglibProxy = cglibFactory.getProxy();

        assertThat(Aop02Proxies.exercise1(jdkProxy)).isEqualTo("JDK");
        assertThat(Aop02Proxies.exercise1(cglibProxy)).isEqualTo("CGLIB");
        assertThat(Aop02Proxies.exercise1(plain)).isEqualTo("BRAK PROXY");
    }

    @Test
    @DisplayName("Ćwiczenie 2: reserveAll woła reserve przez proxy — licznik porady rośnie dla każdego SKU")
    void exercise2() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
                .withBean(CallCounter.class)
                .withBean(CountingAspect.class)
                .withBean("aop02Exercise2Service", Aop02Exercise2Service.class)
                .run(context -> {
                    Aop02Exercise2Service service = context.getBean(Aop02Exercise2Service.class);

                    List<String> results = service.reserveAll(List.of("SPO-001", "KSI-001", "ELE-003"));

                    assertThat(results).containsExactly(
                            "zarezerwowano 1 x SPO-001", "zarezerwowano 1 x KSI-001", "zarezerwowano 1 x ELE-003");
                    assertThat(context.getBean(CallCounter.class).count("reserve"))
                            .as("każde reserve powinno przejść przez proxy").isEqualTo(3);
                });
    }

    @Test
    @DisplayName("Ćwiczenie 3: ręcznie zbudowane proxy JDK loguje wywołanie i nie jest instancją klasy")
    void exercise3() {
        List<String> log = new ArrayList<>();

        PriceCalculator proxy = Aop02Proxies.exercise3(new StandardPriceCalculator(), log);

        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
        assertThat(proxy.priceFor("KSI-001", 2)).isEqualByComparingTo("158.00");
        assertThat(log).containsExactly("przed priceFor", "po priceFor");
        Object asObject = proxy;
        assertThatThrownBy(() -> ((StandardPriceCalculator) asObject).currency()).isInstanceOf(ClassCastException.class);
    }
}
