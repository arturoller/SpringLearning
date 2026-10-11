package s08_data_jpa.jpa04_custom_queries;

/**
 * CategoryStock — projekcja interfejsowa dla zapytania NATYWNEGO z grupowaniem: kategoria i suma sztuk.
 * Nazwy getterów (getCategory, getTotalStock) muszą pasować do ALIASÓW kolumn w SQL (as category, as totalStock).
 */
public interface CategoryStock {

    String getCategory();

    long getTotalStock();
}
