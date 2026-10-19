package s22_extras.ext02_messaging;

/**
 * Ślad odebrania wiadomości przez słuchacza: kto (grupa), co (zdarzenie), skąd (partycja, offset).
 * Służy testom i zapytaniu GET /api/orders/received.
 *
 * @param group     grupa konsumentów (magazyn, faktury)
 * @param event     odebrane zdarzenie
 * @param partition numer partycji tematu
 * @param offset    pozycja wiadomości w partycji
 */
public record ReceivedOrder(String group, OrderPlaced event, int partition, long offset) {
}
