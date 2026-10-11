package s01_core_ioc.ioc04_bean_methods;

import org.springframework.context.annotation.Bean;

import java.time.Clock;

/** Szkielet ĆWICZENIA 3 (patrz Ioc04BeanMethods.exercise3). Klasa BEZ {@code @Configuration} — rejestruje ją test. */
public class Ioc04Exercise3Config {

    @Bean
    public InvoiceNumberGenerator invoiceNumberGenerator(Clock clock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
