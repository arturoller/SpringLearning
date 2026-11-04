package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sekcja 5 lekcji Tx01Transactional: {@code @Transactional} na KLASIE i na METODZIE.
 * Adnotacja na klasie obowiązuje wszystkie publiczne metody; adnotacja na metodzie ZASTĘPUJE ją w całości
 * (nie łączy atrybutów!). Typowy wzorzec: klasa {@code readOnly = true}, metody zapisujące — zwykłe {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true) // read only = tylko do odczytu — domyślne ustawienie dla każdej metody tej klasy
public class WalletSettingsService {

    private final WalletRepository wallets;

    public WalletSettingsService(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /** Dziedziczy ustawienia klasy: transakcja aktywna i readOnly = true (test). */
    public TransactionInfo inspectRead() {
        return TransactionInfo.now();
    }

    /** Adnotacja metody zastępuje klasową: readOnly = false (wartość domyślna), więc zmiana zostanie zapisana (test). */
    @Transactional
    public TransactionInfo rename(Long walletId, String newOwner) {
        wallets.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId)).rename(newOwner);
        return TransactionInfo.now();
    }
}
