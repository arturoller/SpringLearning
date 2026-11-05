package s06_error_handling.err02_exception_handler;

/** Wyjątek domenowy: kupon rabatowy stracił ważność (sekcja 2: pułapka z zapomnianym statusem). */
public class CouponExpiredException extends RuntimeException {

    public CouponExpiredException() {
        super("Kupon rabatowy stracił ważność");
    }
}
