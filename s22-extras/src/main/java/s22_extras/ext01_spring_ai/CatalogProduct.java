package s22_extras.ext01_spring_ai;

import java.math.BigDecimal;

/**
 * Produkt z katalogu sklepu (lekcja Ext01SpringAi). Cena w złotych jako {@code BigDecimal}.
 * To są DANE z naszej bazy — model AI dostaje je w prompcie, ale nigdy ich nie zmienia (cena, stan magazynu
 * i inne fakty pochodzą z aplikacji, nie z modelu).
 */
public record CatalogProduct(String sku, String name, String category, BigDecimal price) {
}
