package s01_core_ioc.ioc04_bean_methods;

import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Szkielet ĆWICZENIA 2 (patrz Ioc04BeanMethods.exercise2). Klasa celowo BEZ {@code @Configuration}: test rejestruje
 * ją ręcznie, więc aplikacja lekcji jej nie skanuje. Metody {@code @Bean} w zwykłej klasie działają w trybie lekkim.
 */
public class Ioc04Exercise2Config {

    @Bean
    public Clock warsawClock() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
