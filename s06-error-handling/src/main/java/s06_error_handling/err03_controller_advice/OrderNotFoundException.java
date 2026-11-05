package s06_error_handling.err03_controller_advice;

/** Wyjątek domenowy sklepu (nie wie nic o HTTP — kod odpowiedzi wybiera advice). */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(long orderId) {
        super("Nie ma zamówienia o id " + orderId);
    }
}
