package s11_testing.test05_test_configuration;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * EuroPriceCalculator = przelicza cenę w złotych na euro po kursie z {@link ExchangeRateClient}
 * (cena / kurs, zaokrąglenie do centów). Serwis, który zależy od zewnętrznego API — w testach adres API podaje
 * {@code @DynamicPropertySource} (zaślepka) albo cały klient jest podmieniany.
 */
@Service
public class EuroPriceCalculator {

    private final ExchangeRateClient rates;

    public EuroPriceCalculator(ExchangeRateClient rates) {
        this.rates = rates;
    }

    public BigDecimal toEuro(BigDecimal pricePln) {
        return pricePln.divide(rates.eurRate(), 2, RoundingMode.HALF_UP);
    }
}
