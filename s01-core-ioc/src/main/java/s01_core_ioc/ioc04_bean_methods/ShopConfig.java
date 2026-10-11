package s01_core_ioc.ioc04_bean_methods;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.NumberFormat;
import java.time.Clock;
import java.util.Locale;

/** Konfiguracja sklepu: beany z klas, których nie możemy oznaczyć adnotacją (Clock, NumberFormat), i generator faktur. */
@Configuration
public class ShopConfig {

    /** Zegar systemowy w domyślnej strefie czasowej. Nazwa beana = nazwa metody: "clock". */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Bean z własną nazwą: {@code @Bean("plnFormat")} zamiast domyślnej "currencyFormat". */
    @Bean("plnFormat")
    public NumberFormat currencyFormat() {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pl-PL"));
    }

    /** Zależność (Clock) przychodzi jako PARAMETR metody — kontener poda bean "clock". */
    @Bean
    public InvoiceNumberGenerator invoiceNumberGenerator(Clock clock) {
        return new InvoiceNumberGenerator(clock);
    }
}
