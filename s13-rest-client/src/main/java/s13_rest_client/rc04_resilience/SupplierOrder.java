package s13_rest_client.rc04_resilience;

/** SupplierOrder = zamówienie u dostawcy: ciało zapytania POST, JSON {"sku":"KSI-001","quantity":3}. */
public record SupplierOrder(String sku, int quantity) {
}
