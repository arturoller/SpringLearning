package s07_data_jdbc.jdbc05_repository;

import java.math.BigDecimal;

/**
 * Produkt sklepu — niezmienny rekord. {@code id} jest {@code null} przed zapisem do bazy (numer nadaje baza),
 * {@code version} to numer wersji wiersza dla optymistycznego blokowania (sekcja 5 lekcji).
 * Zmiana = nowy obiekt: metody {@code with…} zwracają kopię z jednym polem zmienionym.
 */
public record Product(Long id, String name, String category, BigDecimal price, int stock, long version) {

    /** Nowy produkt jeszcze niezapisany: bez numeru, wersja 0. */
    public static Product create(String name, String category, BigDecimal price, int stock) {
        return new Product(null, name, category, price, stock, 0);
    }

    public Product withStock(int newStock) {
        return new Product(id, name, category, price, newStock, version);
    }

    public Product withPrice(BigDecimal newPrice) {
        return new Product(id, name, category, newPrice, stock, version);
    }
}
