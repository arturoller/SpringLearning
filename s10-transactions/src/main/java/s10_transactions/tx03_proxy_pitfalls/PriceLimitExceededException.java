package s10_transactions.tx03_proxy_pitfalls;

import java.math.BigDecimal;

/** Cena po podwyżce przekroczyłaby limit (price limit exceeded = przekroczony limit ceny). Niesprawdzany. */
public class PriceLimitExceededException extends RuntimeException {

    public PriceLimitExceededException(String product, BigDecimal newPrice) {
        super("Cena produktu " + product + " przekroczyłaby limit: " + newPrice);
    }
}
