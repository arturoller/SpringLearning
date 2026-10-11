package s09_jpa_relations.rel03_many_to_many;

/**
 * ProductSales = sprzedaż produktu: nazwa i łączna sprzedana ilość. Rekord (record) jako wynik zapytania JPQL
 * z wyrażeniem konstruktora {@code select new ...ProductSales(...)} — sekcja 5 lekcji.
 */
public record ProductSales(String productName, long totalQuantity) {
}
