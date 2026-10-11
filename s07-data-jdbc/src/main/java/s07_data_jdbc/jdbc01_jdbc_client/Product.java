package s07_data_jdbc.jdbc01_jdbc_client;

/**
 * Produkt ze sklepu — niezmienny rekord (record). Nazwy komponentów odpowiadają kolumnom tabeli:
 * kolumna PRICE_CENTS trafia do komponentu {@code priceCents} (podkreślniki są zamieniane na camelCase).
 *
 * @param id         klucz główny (primary key)
 * @param name       nazwa produktu
 * @param priceCents cena w groszach (liczby całkowite, żeby uniknąć błędów zaokrągleń)
 * @param stock      liczba sztuk w magazynie
 */
public record Product(long id, String name, int priceCents, int stock) {
}
