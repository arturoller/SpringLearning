package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Wypłata z opłatą (fee = opłata) — sekcja 3 lekcji Tx01Transactional: KTÓRE wyjątki wycofują transakcję.
 * Każda metoda najpierw pobiera opłatę 2.00 (zmiana encji w transakcji), a potem rzuca wyjątek, gdy brakuje środków.
 * Pytanie brzmi zawsze tak samo: czy opłata zostanie w bazie?
 */
@Service
public class FeeService {

    public static final BigDecimal FEE = new BigDecimal("2.00");

    private final WalletRepository wallets;

    public FeeService(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /** Domyślnie + wyjątek SPRAWDZANY → COMMIT: opłata zostaje, choć metoda „się nie udała” (test). */
    @Transactional
    public void withdrawWithFee(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        Wallet wallet = chargeFee(walletId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(walletId, amount, wallet.getBalance());
        }
        wallet.withdraw(amount);
    }

    /** {@code rollbackFor} (rollback for = wycofaj dla) — wyjątek sprawdzany też wycofuje: opłaty nie ma (test). */
    @Transactional(rollbackFor = InsufficientFundsException.class)
    public void withdrawWithFeeOrRollback(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        Wallet wallet = chargeFee(walletId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(walletId, amount, wallet.getBalance());
        }
        wallet.withdraw(amount);
    }

    /**
     * {@code noRollbackFor} (no rollback for = nie wycofuj dla) — wyjątek NIESPRAWDZANY, a mimo to COMMIT: opłata za
     * próbę wypłaty zostaje celowo (reguła biznesowa „próba też kosztuje”). Test.
     */
    @Transactional(noRollbackFor = WithdrawalRejectedException.class)
    public void withdrawOrKeepFee(Long walletId, BigDecimal amount) {
        Wallet wallet = chargeFee(walletId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new WithdrawalRejectedException("Odrzucono wypłatę " + amount + " z portfela " + walletId);
        }
        wallet.withdraw(amount);
    }

    /** {@code Error} (np. OutOfMemoryError) domyślnie WYCOFUJE — jak RuntimeException (test). */
    @Transactional
    public void withdrawWithFeeAndCrash(Long walletId) {
        chargeFee(walletId);
        throw new LedgerCorruptedError("Księga portfela " + walletId + " jest uszkodzona");
    }

    private Wallet chargeFee(Long walletId) {
        Wallet wallet = wallets.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
        wallet.withdraw(FEE); // bez save(): encja jest zarządzana, Hibernate wyśle UPDATE przy commicie (dirty checking)
        return wallet;
    }
}
