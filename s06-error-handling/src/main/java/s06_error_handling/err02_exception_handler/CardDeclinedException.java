package s06_error_handling.err02_exception_handler;

/** Bank odrzucił kartę — szczególny przypadek błędu płatności (podklasa PaymentException). */
public class CardDeclinedException extends PaymentException {

    public CardDeclinedException(String lastDigits) {
        super("Karta kończąca się na " + lastDigits + " została odrzucona");
    }
}
