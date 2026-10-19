package s04_web_rest.rest05_crud_api;

import java.math.BigDecimal;

/**
 * Product = MODEL (model dziedziny) — produkt tak, jak widzi go sklep od środka. Ma pole purchasePrice (cena zakupu
 * od hurtowni) — tajemnica handlowa, która NIE może wyjść do klienta API. Dlatego kontroler nigdy nie zwraca Product,
 * tylko ProductResponse (DTO). Rekord — niezmienny; „zmiana” = nowy obiekt (with...).
 */
public record Product(String sku, String name, Category category, BigDecimal price, BigDecimal purchasePrice,
                      int stock) {

    /** Kategorie sklepu. */
    public enum Category {
        ELEKTRONIKA, SPOZYWCZE, KSIAZKI, ODZIEZ, DOM
    }

    Product withName(String newName) {
        return new Product(sku, newName, category, price, purchasePrice, stock);
    }

    Product withPrice(BigDecimal newPrice) {
        return new Product(sku, name, category, newPrice, purchasePrice, stock);
    }

    Product withStock(int newStock) {
        return new Product(sku, name, category, price, purchasePrice, newStock);
    }
}
