package s10_transactions.tx04_read_only_isolation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Sekcja 4 lekcji Tx04ReadOnlyIsolation: blokowanie PESYMISTYCZNE. Wiersz portfela jest blokowany już przy odczycie
 * ({@code select … for update}) i zostaje zablokowany do końca transakcji — nikt inny nie zmieni go „w międzyczasie”.
 */
@Service
public class LockingService {

    private final WalletRepository wallets;

    public LockingService(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /** Wypłata z blokadą: odczyt z blokadą → {@code meanwhile} (ktoś inny próbuje) → sprawdzenie salda → zapis. */
    @Transactional
    public BigDecimal withdrawWithLock(long walletId, BigDecimal amount, Runnable meanwhile) {
        Wallet wallet = wallets.findForUpdateById(walletId).orElseThrow();
        meanwhile.run();
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Za mało środków w portfelu " + walletId);
        }
        wallet.withdraw(amount);
        return wallet.getBalance();
    }
}
