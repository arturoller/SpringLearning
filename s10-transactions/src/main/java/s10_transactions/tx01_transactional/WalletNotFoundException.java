package s10_transactions.tx01_transactional;

/**
 * Brak portfela o danym id (not found = nie znaleziono). Wyjątek NIESPRAWDZANY (unchecked — dziedziczy po
 * {@code RuntimeException}), więc domyślnie WYCOFUJE transakcję (sekcja 3 lekcji).
 */
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(Long walletId) {
        super("Nie ma portfela o id " + walletId);
    }
}
