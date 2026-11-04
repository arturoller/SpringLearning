package s10_transactions.tx03_proxy_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import s10_transactions.tx03_pitfalls.expose_proxy.ExposeProxyApplication;
import s10_transactions.tx03_pitfalls.expose_proxy.ExposingService;
import s10_transactions.tx03_pitfalls.final_class.FinalClassApplication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx03ProxyPitfalls z NIETYPOWĄ konfiguracją — każdy uruchamia własną aplikację z własną bazą:
 * proxy JDK ({@code spring.aop.proxy-target-class=false}), klasa final z {@code @Transactional} (start się nie udaje)
 * i {@code exposeProxy = true}. Aplikacje-pułapki leżą w pakiecie {@code s10_transactions.tx03_pitfalls}.
 */
class Tx03SpecialSetupTest {

    private static String uniqueDb() {
        return "--spring.datasource.url=jdbc:h2:mem:tx03-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("Sekcja 1: proxy-target-class=false — bean z interfejsem dostaje proxy JDK, dostępne tylko jako interfejs")
    void section1JdkProxy() {
        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(Tx03Application.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=tx03", uniqueDb(), "--spring.aop.proxy-target-class=false")) {
            DiscountPolicy policy = app.getBean(DiscountPolicy.class);

            assertThat(AopUtils.isJdkDynamicProxy(policy)).isTrue();
            assertThat(policy.getClass().getName()).startsWith("jdk.proxy").contains("$Proxy");
            assertThat(policy).isNotInstanceOf(SeasonalDiscountPolicy.class);
            assertThat(policy.discountPercent("Monitor")).isEqualTo(10);
            assertThatThrownBy(() -> app.getBean(SeasonalDiscountPolicy.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                    .hasMessage("No qualifying bean of type 's10_transactions.tx03_proxy_pitfalls.SeasonalDiscountPolicy' available");

            // klasa bez interfejsu i tak dostaje CGLIB — JDK proxy potrzebuje interfejsu
            assertThat(AopUtils.isCglibProxy(app.getBean(PriceService.class))).isTrue();
        }
    }

    @Test
    @DisplayName("Sekcja 3: klasa final z @Transactional — aplikacja nie startuje (CGLIB nie zrobi podklasy)")
    void section3FinalClassBreaksStartup() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(FinalClassApplication.class).web(WebApplicationType.NONE)
                .run(uniqueDb()))
                .isInstanceOf(BeanCreationException.class)
                .hasMessageContaining("Could not generate CGLIB subclass of class "
                        + "s10_transactions.tx03_pitfalls.final_class.FinalPriceService: "
                        + "Common causes of this problem include using a final class or a non-visible class")
                .hasCauseInstanceOf(AopConfigException.class)
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot subclass final class s10_transactions.tx03_pitfalls.final_class.FinalPriceService");
    }

    @Test
    @DisplayName("Sekcja 2: exposeProxy = true — AopContext.currentProxy() zwraca proxy i transakcja działa")
    void section2ExposeProxy() {
        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(ExposeProxyApplication.class)
                .web(WebApplicationType.NONE).run(uniqueDb())) {
            ExposingService service = app.getBean(ExposingService.class);

            assertThat(service.viaThis()).isFalse();
            assertThat(service.viaCurrentProxy()).isTrue();
        }
    }
}
