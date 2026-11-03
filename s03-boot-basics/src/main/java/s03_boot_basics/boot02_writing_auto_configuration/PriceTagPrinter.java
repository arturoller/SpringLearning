package s03_boot_basics.boot02_writing_auto_configuration;

import java.math.BigDecimal;

/** Drukarka etykiet cenowych — potrzebuje {@link PriceFormatter}, więc ma sens tylko wtedy, gdy formater istnieje. */
public class PriceTagPrinter {

    private final PriceFormatter formatter;

    public PriceTagPrinter(PriceFormatter formatter) {
        this.formatter = formatter;
    }

    /** Np. {@code tag("Kubek", 19.9) → "[Kubek | 19,90 zł]"}. */
    public String tag(String productName, BigDecimal price) {
        return "[" + productName + " | " + formatter.format(price) + "]";
    }
}
