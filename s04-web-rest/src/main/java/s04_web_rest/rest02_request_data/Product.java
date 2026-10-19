package s04_web_rest.rest02_request_data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product = produkt sklepu (rekord — niezmienny). Cena w złotych jako BigDecimal.
 * ALL — pełna lista produktów sklepu z kursu (tylko do odczytu, List.of jest niezmienna).
 */
public record Product(String sku, String name, Category category, BigDecimal price, int stock) {

    public static final List<Product> ALL = List.of(
            new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7),
            new Product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, new BigDecimal("2999.00"), 0),
            new Product("ELE-003", "Słuchawki BT", Category.ELEKTRONIKA, new BigDecimal("349.90"), 25),
            new Product("ELE-004", "Monitor 27 cali", Category.ELEKTRONIKA, new BigDecimal("1299.00"), 4),
            new Product("SPO-001", "Kawa ziarnista 1kg", Category.SPOZYWCZE, new BigDecimal("64.99"), 120),
            new Product("SPO-002", "Czekolada gorzka", Category.SPOZYWCZE, new BigDecimal("7.49"), 300),
            new Product("SPO-003", "Oliwa z oliwek", Category.SPOZYWCZE, new BigDecimal("42.00"), 0),
            new Product("KSI-001", "Czysty kod", Category.KSIAZKI, new BigDecimal("79.00"), 15),
            new Product("KSI-002", "Java. Podstawy", Category.KSIAZKI, new BigDecimal("129.00"), 9),
            new Product("KSI-003", "Wzorce projektowe", Category.KSIAZKI, new BigDecimal("99.00"), 3),
            new Product("ODZ-001", "Kurtka zimowa", Category.ODZIEZ, new BigDecimal("459.00"), 12),
            new Product("ODZ-002", "T-shirt bawełniany", Category.ODZIEZ, new BigDecimal("49.99"), 80),
            new Product("DOM-001", "Ekspres do kawy", Category.DOM, new BigDecimal("1899.00"), 2),
            new Product("DOM-002", "Lampka biurkowa", Category.DOM, new BigDecimal("129.00"), 18));
}
