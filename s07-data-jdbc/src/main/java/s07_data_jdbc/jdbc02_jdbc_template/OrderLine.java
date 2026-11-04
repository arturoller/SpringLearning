package s07_data_jdbc.jdbc02_jdbc_template;

import java.math.BigDecimal;

/** Pozycja zamówienia: który produkt, ile sztuk i po jakiej cenie jednostkowej w chwili zakupu. */
public record OrderLine(Long productId, int quantity, BigDecimal unitPrice) {
}
