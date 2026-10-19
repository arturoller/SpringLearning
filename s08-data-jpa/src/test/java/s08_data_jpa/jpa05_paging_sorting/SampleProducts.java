package s08_data_jpa.jpa05_paging_sorting;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dane sklepu do testów lekcji Jpa05PagingSorting — 14 produktów (te same co w całym kursie i w JavaLearning).
 * Każde wywołanie {@link #all()} tworzy NOWE obiekty: encje zapisane w jednym teście nie mogą trafić do drugiego.
 */
public final class SampleProducts {

    private SampleProducts() {
    }

    public static List<Product> all() {
        return List.of(
                product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, "5499.99", 7),
                product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, "2999.00", 0),
                product("ELE-003", "Słuchawki BT", Category.ELEKTRONIKA, "349.90", 25),
                product("ELE-004", "Monitor 27 cali", Category.ELEKTRONIKA, "1299.00", 4),
                product("SPO-001", "Kawa ziarnista 1kg", Category.SPOZYWCZE, "64.99", 120),
                product("SPO-002", "Czekolada gorzka", Category.SPOZYWCZE, "7.49", 300),
                product("SPO-003", "Oliwa z oliwek", Category.SPOZYWCZE, "42.00", 0),
                product("KSI-001", "Czysty kod", Category.KSIAZKI, "79.00", 15),
                product("KSI-002", "Java. Podstawy", Category.KSIAZKI, "129.00", 9),
                product("KSI-003", "Wzorce projektowe", Category.KSIAZKI, "99.00", 3),
                product("ODZ-001", "Kurtka zimowa", Category.ODZIEZ, "459.00", 12),
                product("ODZ-002", "T-shirt bawełniany", Category.ODZIEZ, "49.99", 80),
                product("DOM-001", "Ekspres do kawy", Category.DOM, "1899.00", 2),
                product("DOM-002", "Lampka biurkowa", Category.DOM, "129.00", 18));
    }

    private static Product product(String sku, String name, Category category, String price, int stock) {
        return new Product(sku, name, category, new BigDecimal(price), stock);
    }
}
