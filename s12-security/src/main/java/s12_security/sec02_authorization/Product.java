package s12_security.sec02_authorization;

import java.math.BigDecimal;

/** Produkt w katalogu sklepu (lekcja Sec02Authorization). */
public record Product(long id, String name, BigDecimal price) {
}
