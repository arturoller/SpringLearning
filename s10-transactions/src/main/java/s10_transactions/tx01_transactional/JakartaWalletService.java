package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Sekcja 5 lekcji Tx01Transactional: {@code jakarta.transaction.Transactional} (adnotacja ze standardu Jakarta EE,
 * nie ze Springa). Spring ją rozumie (sprawdzone testem w Boot 4.1.1), ale ma INNE atrybuty:
 * {@code rollbackOn} zamiast {@code rollbackFor}, {@code dontRollbackOn} zamiast {@code noRollbackFor},
 * {@code value = TxType.REQUIRES_NEW} zamiast {@code propagation} — i NIE ma {@code readOnly}, {@code timeout}
 * ani {@code isolation}. Domyślna reguła wycofania jest taka sama: checked → commit.
 */
@Service
public class JakartaWalletService {

    private final WalletRepository wallets;

    public JakartaWalletService(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /** Transakcja jest — nazwa jak przy springowej adnotacji (test). */
    @jakarta.transaction.Transactional
    public TransactionInfo inspect() {
        return TransactionInfo.now();
    }

    /** Domyślnie: wyjątek sprawdzany → COMMIT (opłata zostaje) — tak samo jak w FeeService.withdrawWithFee (test). */
    @jakarta.transaction.Transactional
    public void withdrawWithFee(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        withdrawOrThrow(walletId, amount);
    }

    /** {@code rollbackOn} (rollback on = wycofaj przy) — odpowiednik springowego rollbackFor (test). */
    @jakarta.transaction.Transactional(rollbackOn = InsufficientFundsException.class)
    public void withdrawWithFeeOrRollback(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        withdrawOrThrow(walletId, amount);
    }

    private void withdrawOrThrow(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        Wallet wallet = wallets.findById(walletId).orElseThrow(() -> new WalletNotFoundException(walletId));
        wallet.withdraw(FeeService.FEE);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(walletId, amount, wallet.getBalance());
        }
        wallet.withdraw(amount);
    }
}
