package s10_transactions.tx04_read_only_isolation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Sekcja 3 lekcji Tx04ReadOnlyIsolation: lost update (utracona aktualizacja). Schemat „przeczytaj — policz — zapisz”
 * w dwóch transakcjach naraz: obie czytają 100.00, każda dodaje swoją kwotę, wygrywa ta, która zapisze OSTATNIA.
 * {@code meanwhile} to miejsce, w którym „w tym samym czasie” działa drugi użytkownik (OtherUser).
 */
@Service
public class TopUpService {

    private final WalletRepository wallets;
    private final VersionedWalletRepository versionedWallets;

    public TopUpService(WalletRepository wallets, VersionedWalletRepository versionedWallets) {
        this.wallets = wallets;
        this.versionedWallets = versionedWallets;
    }

    /** PRZED: portfel bez wersji — cudza zmiana z {@code meanwhile} zostanie nadpisana (UPDATE … SET balance = 130.00). */
    @Transactional
    public BigDecimal topUp(long walletId, BigDecimal amount, Runnable meanwhile) {
        Wallet wallet = wallets.findById(walletId).orElseThrow();
        meanwhile.run();
        wallet.deposit(amount);
        return wallet.getBalance();
    }

    /** PO: portfel z {@code @Version} — UPDATE z warunkiem na wersję trafia w 0 wierszy → konflikt przy commicie. */
    @Transactional
    public BigDecimal topUpVersioned(long walletId, BigDecimal amount, Runnable meanwhile) {
        VersionedWallet wallet = versionedWallets.findById(walletId).orElseThrow();
        meanwhile.run();
        wallet.deposit(amount);
        return wallet.getBalance();
    }

    /** Dla porównania: portfel BEZ wersji, ale transakcja z REPEATABLE READ — konflikt wykrywa sama baza H2. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal topUpRepeatableRead(long walletId, BigDecimal amount, Runnable meanwhile) {
        Wallet wallet = wallets.findById(walletId).orElseThrow();
        meanwhile.run();
        wallet.deposit(amount);
        return wallet.getBalance();
    }
}
