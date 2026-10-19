package s13_rest_client.rc01_rest_client;

/**
 * SupplierOrder = zamówienie u dostawcy (order = zamówienie): ciało (body) zapytania POST, które wysyła NASZ sklep.
 * RestClient zamienia ten rekord na JSON: {"sku":"KSI-001","quantity":3}.
 */
public record SupplierOrder(String sku, int quantity) {
}
