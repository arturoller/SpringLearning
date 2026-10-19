package s22_extras.ext02_messaging;

import java.math.BigDecimal;
import java.util.List;

/**
 * Zdarzenie „złożono zamówienie” (lekcja Ext02Messaging) — wiadomość wysyłana do Kafki jako JSON.
 * Rekord jest niezmienny, więc nadaje się na wiadomość: nikt nie zmieni jej po wysłaniu.
 *
 * @param orderId    numer zamówienia, np. ZAM-001 — także KLUCZ wiadomości (ten sam klucz = ta sama partycja)
 * @param customerId id klienta
 * @param skus       kody zamówionych produktów
 * @param total      suma w złotych
 */
public record OrderPlaced(String orderId, long customerId, List<String> skus, BigDecimal total) {
}
