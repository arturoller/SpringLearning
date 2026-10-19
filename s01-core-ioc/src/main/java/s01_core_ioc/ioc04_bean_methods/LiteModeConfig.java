package s01_core_ioc.ioc04_bean_methods;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tryb LEKKI ("lite"): {@code @Configuration(proxyBeanMethods = false)}. Bez proxy metody {@code @Bean} są zwykłymi
 * metodami Javy, więc wywołanie {@code liteTaxTable()} z innej metody tworzy NOWY obiekt (poza kontenerem).
 * Taki sam efekt daje klasa z metodami {@code @Bean} bez żadnej adnotacji klasowej.
 */
@Configuration(proxyBeanMethods = false)
public class LiteModeConfig {

    @Bean
    public TaxTable liteTaxTable() {
        return new TaxTable();
    }

    @Bean
    public PriceCalculator liteCalculator() {
        return new PriceCalculator(liteTaxTable()); // zwykłe wywołanie — NOWA TaxTable, nie bean "liteTaxTable"
    }

    /**
     * Poprawny sposób w trybie lekkim: zależność jako PARAMETR metody (kontener poda beana).
     * W aplikacji lekcji są DWA beany TaxTable (fullTaxTable z FullModeConfig i liteTaxTable stąd), dlatego
     * {@code @Qualifier("liteTaxTable")} (kwalifikator — wskazanie beana po nazwie) mówi wprost, którego chcemy.
     * Bez niego zadziałałoby tylko dopasowanie po NAZWIE parametru (kruche — patrz Ioc05MultipleCandidates):
     * parametr nazwany np. taxTable wywróciłby start aplikacji (NoUniqueBeanDefinitionException).
     */
    @Bean
    public PriceCalculator liteCalculatorFromParameter(@Qualifier("liteTaxTable") TaxTable taxTable) {
        return new PriceCalculator(taxTable);
    }
}
