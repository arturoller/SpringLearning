package s03_boot_basics.boot02_writing_auto_configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * „Biblioteka” lekcji: formatowanie cen sklepu, np. {@code 12.5 → "12,50 zł"} (formatter = formater, „ładnie wypisujący”).
 * <p>
 * Zwykła klasa Javy — bez adnotacji Springa. O tym, czy i jak stanie się beanem, decyduje autokonfiguracja
 * {@link PriceAutoConfiguration}. Dzięki temu da się jej używać także bez Springa (zwykłe {@code new}).
 * Formatowanie jest celowo ręczne (bez {@code Locale}), żeby wynik nie zależał od ustawień komputera.
 */
public class PriceFormatter {

    private final String currency;
    private final int decimals;

    public PriceFormatter(String currency, int decimals) {
        this.currency = currency;
        this.decimals = decimals;
    }

    /** Zaokrągla „po szkolnemu” (HALF_UP), zamienia kropkę na przecinek i dopisuje walutę. */
    public String format(BigDecimal amount) {
        String number = amount.setScale(decimals, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        return number + " " + currency;
    }

    public String currency() {
        return currency;
    }

    public int decimals() {
        return decimals;
    }
}
