package s01_core_ioc.solutions.ioc04_bean_methods;

import org.springframework.context.annotation.Bean;
import s01_core_ioc.ioc04_bean_methods.InvoiceNumberGenerator;

import java.time.Clock;

/** Rozwiązanie ćwiczenia 3: generator faktur zbudowany z zegara podanego jako parametr metody. */
public class Ioc04Exercise3ConfigSolution {

    @Bean
    public InvoiceNumberGenerator invoiceNumberGenerator(Clock clock) {
        return new InvoiceNumberGenerator(clock);
    }
}
