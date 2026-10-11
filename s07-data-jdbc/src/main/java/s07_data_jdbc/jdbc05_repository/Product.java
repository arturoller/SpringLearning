package s07_data_jdbc.jdbc05_repository;

/**
 * Produkt ze sklepu — niezmienny rekord (record), wynik odczytu z bazy.
 *
 * @param id         klucz główny nadany przez bazę
 * @param name       unikalna nazwa produktu
 * @param priceCents cena w groszach
 */
public record Product(long id, String name, int priceCents) {
}
