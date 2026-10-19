package s18_aop.aop02_proxies;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Konfiguracja CZYSTEGO Springa (bez Spring Boota) — do porównania z Bootem w sekcji 2.
 * {@code @EnableAspectJAutoProxy} bez parametrów = proxyTargetClass false = proxy JDK dla klas z interfejsem.
 * Klasa NIE ma {@code @Configuration}, więc skanowanie Aop02Application jej nie znajdzie (inaczej jej
 * {@code @EnableAspectJAutoProxy} trafiłoby też do kontekstu lekcji). Test tworzy z niej osobny kontekst:
 * {@code new AnnotationConfigApplicationContext(PlainSpringConfig.class)}.
 */
@EnableAspectJAutoProxy
public class PlainSpringConfig {

    @Bean
    CallCounter callCounter() {
        return new CallCounter();
    }

    @Bean
    CountingAspect countingAspect(CallCounter callCounter) {
        return new CountingAspect(callCounter);
    }

    @Bean
    StandardPriceCalculator standardPriceCalculator() {
        return new StandardPriceCalculator();
    }
}
