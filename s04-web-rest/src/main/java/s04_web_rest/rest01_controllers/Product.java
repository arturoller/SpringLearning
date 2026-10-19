package s04_web_rest.rest01_controllers;

import java.math.BigDecimal;

/**
 * Product = produkt sklepu (dane jak w całym kursie: ELE-001 Laptop Pro 14…). Rekord — niezmienny (immutable).
 * Jackson (biblioteka JSON w Springu) zamienia go na JSON
 * {"sku":"ELE-001","name":"Laptop Pro 14","category":"ELEKTRONIKA","price":5499.99,"stock":7} i z powrotem.
 * Cena w złotych jako BigDecimal — dokładna arytmetyka pieniędzy (t15_numbers/Numbers01BigDecimal).
 */
public record Product(String sku, String name, String category, BigDecimal price, int stock) {

    /** Kopia produktu z innym stanem magazynu — rekord jest niezmienny, więc „zmiana” = nowy obiekt. */
    Product withStock(int newStock) {
        return new Product(sku, name, category, price, newStock);
    }
}
