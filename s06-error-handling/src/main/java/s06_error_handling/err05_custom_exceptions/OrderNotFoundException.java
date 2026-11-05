package s06_error_handling.err05_custom_exceptions;

/** Brak zamówienia o podanym id. */
public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException(long orderId) {
        super("ORDER_NOT_FOUND", "Nie ma zamówienia o id " + orderId);
    }
}
