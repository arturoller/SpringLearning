package s20_modulith.mod02_module_events.orders;

/**
 * Zdarzenie modułu {@code orders}: „zamówienie złożone” (placed = złożone). Część API modułu — leży w pakiecie
 * bazowym, bo inne moduły muszą znać ten typ, żeby go słuchać. Rekord = niezmienny, więc słuchacz w innym wątku
 * dostaje dokładnie to, co opublikowano. Nazwa w czasie przeszłym: zdarzenie opisuje FAKT, który już zaszedł.
 *
 * @param orderId  numer zamówienia, np. ZAM-001
 * @param sku      kod produktu
 * @param quantity liczba sztuk
 */
public record OrderPlaced(String orderId, String sku, int quantity) {
}
