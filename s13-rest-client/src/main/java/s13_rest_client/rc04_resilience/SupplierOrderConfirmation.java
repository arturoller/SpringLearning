package s13_rest_client.rc04_resilience;

/** SupplierOrderConfirmation = potwierdzenie zamówienia: {"orderId":"DOST-1001","sku":"KSI-001","quantity":3,"status":"PRZYJETE"}. */
public record SupplierOrderConfirmation(String orderId, String sku, int quantity, String status) {
}
