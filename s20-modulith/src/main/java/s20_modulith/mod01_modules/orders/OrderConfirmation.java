package s20_modulith.mod01_modules.orders;

import java.math.BigDecimal;

/**
 * Potwierdzenie złożenia zamówienia (confirmation = potwierdzenie; API modułu {@code orders}).
 *
 * @param orderId      numer zamówienia, np. ZAM-001
 * @param customerName imię i nazwisko klienta
 * @param sku          kod produktu
 * @param quantity     liczba sztuk
 * @param total        wartość w PLN (cena × liczba sztuk)
 */
public record OrderConfirmation(String orderId, String customerName, String sku, int quantity, BigDecimal total) {
}
