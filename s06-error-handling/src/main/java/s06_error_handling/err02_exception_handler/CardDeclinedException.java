package s06_error_handling.err02_exception_handler;

/** Szczególny przypadek problemu z płatnością: bank odrzucił kartę. Dziedziczy po {@link PaymentException}. */
public class CardDeclinedException extends PaymentException {

    public CardDeclinedException() {
        super("Karta odrzucona przez bank");
    }
}
