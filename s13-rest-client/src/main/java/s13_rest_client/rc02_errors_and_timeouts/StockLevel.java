package s13_rest_client.rc02_errors_and_timeouts;

/**
 * StockLevel = stan magazynu u dostawcy dla jednego produktu (stock level = poziom zapasu).
 * JSON: {"sku":"ELE-001","available":7}.
 */
public record StockLevel(String sku, int available) {
}
