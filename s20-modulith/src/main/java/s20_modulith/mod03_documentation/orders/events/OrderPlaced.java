package s20_modulith.mod03_documentation.orders.events;

import java.math.BigDecimal;

/**
 * Zdarzenie modułu {@code orders}: zamówienie złożone. Leży w podpakiecie {@code events}, który jest NAZWANYM
 * INTERFEJSEM modułu (zob. package-info.java) — dlatego inne moduły mogą go używać, choć to podpakiet.
 *
 * @param orderId  numer zamówienia
 * @param sku      kod produktu
 * @param quantity liczba sztuk
 * @param total    wartość zamówienia w PLN
 */
public record OrderPlaced(String orderId, String sku, int quantity, BigDecimal total) {
}
