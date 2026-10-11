package s01_core_ioc.ioc04_bean_methods;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tryb PEŁNY ("full"): {@code @Configuration} z domyślnym {@code proxyBeanMethods = true}. Spring tworzy podklasę
 * (proxy CGLIB) tej klasy i przechwytuje wywołania metod {@code @Bean}: wywołanie {@code fullTaxTable()} z innej
 * metody zwraca ISTNIEJĄCEGO singletona z kontenera, a nie tworzy nowego obiektu.
 */
@Configuration
public class FullModeConfig {

    @Bean
    public TaxTable fullTaxTable() {
        return new TaxTable();
    }

    @Bean
    public PriceCalculator fullCalculator() {
        return new PriceCalculator(fullTaxTable()); // wywołanie metody — proxy zwróci beana "fullTaxTable"
    }
}
