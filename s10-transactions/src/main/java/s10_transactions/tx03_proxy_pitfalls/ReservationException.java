package s10_transactions.tx03_proxy_pitfalls;

/**
 * Nie udało się zarezerwować towaru. Wyjątek KONTROLOWANY (checked, dziedziczy po Exception) — domyślnie NIE wycofuje
 * transakcji (Tx01Transactional, sekcja 3). W tej lekcji pokazuje pułapkę „złap niekontrolowany, rzuć kontrolowany”.
 */
public class ReservationException extends Exception {

    public ReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}
