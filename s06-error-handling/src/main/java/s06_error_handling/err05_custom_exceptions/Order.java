package s06_error_handling.err05_custom_exceptions;

/** Zamówienie: produkt, ilość i informacja, czy zostało wysłane. */
public record Order(long id, long productId, int quantity, boolean shipped) {
}
