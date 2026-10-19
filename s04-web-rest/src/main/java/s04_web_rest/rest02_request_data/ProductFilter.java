package s04_web_rest.rest02_request_data;

import java.math.BigDecimal;

/**
 * ProductFilter = filtr wyszukiwania z parametrów zapytania ({@code ?category=...&minPrice=...&maxPrice=...&inStock=...}).
 * Typy obiektowe (Category, BigDecimal, Boolean), a nie prymitywy — brak parametru to null, czyli „nie filtruj”.
 */
public record ProductFilter(Category category, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {

    boolean matches(Product product) {
        return (category == null || product.category() == category)
                && (minPrice == null || product.price().compareTo(minPrice) >= 0)
                && (maxPrice == null || product.price().compareTo(maxPrice) <= 0)
                && (inStock == null || (product.stock() > 0) == inStock);
    }
}
