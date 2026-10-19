package s20_modulith.mod02_module_events.inventory;

/**
 * Zdarzenie modułu {@code inventory} (magazyn): „towar zarezerwowany” (reserved = zarezerwowany). Mogą go słuchać
 * kolejne moduły (np. przyszła wysyłka); w lekcji czeka na nie test modułu ze {@code Scenario}.
 *
 * @param orderId  numer zamówienia
 * @param sku      kod produktu
 * @param quantity liczba zarezerwowanych sztuk
 */
public record StockReserved(String orderId, String sku, int quantity) {
}
