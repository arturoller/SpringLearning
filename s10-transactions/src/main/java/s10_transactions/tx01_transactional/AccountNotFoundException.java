package s10_transactions.tx01_transactional;

/** Nie ma takiego konta. Wyjątek NIEKONTROLOWANY (unchecked, dziedziczy po RuntimeException) → domyślnie rollback. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String number) {
        super("Nie ma konta: " + number);
    }
}
