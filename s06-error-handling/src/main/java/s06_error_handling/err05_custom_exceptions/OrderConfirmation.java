package s06_error_handling.err05_custom_exceptions;

/** Potwierdzenie przyjęcia zamówienia. */
public record OrderConfirmation(long orderId, long productId, int quantity) {
}
