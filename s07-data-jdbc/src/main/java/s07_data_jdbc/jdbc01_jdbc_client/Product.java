package s07_data_jdbc.jdbc01_jdbc_client;

import java.math.BigDecimal;

/**
 * Produkt sklepu (record = rekord, niezmienny nośnik danych). Nazwy pól odpowiadają kolumnom tabeli
 * {@code product}: kolumna {@code sku} → pole {@code sku}, a np. kolumna {@code unit_price} trafiłaby do pola
 * {@code unitPrice} (Spring zamienia snake_case na camelCase). Cena: złote jako {@code BigDecimal}.
 */
public record Product(long id, String sku, String name, String category, BigDecimal price, int stock) {
}
