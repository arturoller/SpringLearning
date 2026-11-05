package s10_transactions.tx04_read_only_isolation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * „Drugi użytkownik” w testach współbieżności lekcji Tx04ReadOnlyIsolation. Każda metoda ma
 * {@code REQUIRES_NEW}: działa we WŁASNEJ transakcji, na DRUGIM połączeniu (s10_transactions/Tx02Propagation, sekcja 3),
 * i zatwierdza się od razu po powrocie. Dzięki temu dwie transakcje przeplatamy RĘCZNIE, w jednym wątku: transakcja A
 * woła w połowie pracy {@code Runnable meanwhile} (meanwhile = w międzyczasie), który uruchamia metodę z tej klasy
 * (transakcja B). Kolejność zdarzeń jest zawsze ta sama — bez wątków, bez sleepów.
 */
@Service
public class OtherUser {

    private final WalletRepository wallets;
    private final VersionedWalletRepository versionedWallets;
    private final JdbcClient jdbc;

    public OtherUser(WalletRepository wallets, VersionedWalletRepository versionedWallets, JdbcClient jdbc) {
        this.wallets = wallets;
        this.versionedWallets = versionedWallets;
        this.jdbc = jdbc;
    }

    /** Doładowanie portfela (przeczytaj — dodaj — zapisz przy commicie). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void topUp(long walletId, BigDecimal amount) {
        wallets.findById(walletId).orElseThrow().deposit(amount);
    }

    /** To samo na portfelu z wersją — Hibernate podbije wersję. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void topUpVersioned(long walletId, BigDecimal amount) {
        versionedWallets.findById(walletId).orElseThrow().deposit(amount);
    }

    /** Doładowanie z blokadą pesymistyczną bez czekania ({@code for update nowait}). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void topUpWithLockNoWait(long walletId, BigDecimal amount) {
        wallets.findForUpdateNoWaitById(walletId).orElseThrow().deposit(amount);
    }

    /** Odczyt salda z poziomem READ_COMMITTED (widzi tylko dane zatwierdzone). */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public BigDecimal readBalance(long walletId) {
        return balanceOf(walletId);
    }

    /** Odczyt salda z poziomem READ_UNCOMMITTED (uncommitted = niezatwierdzone) — widzi też cudze NIEzatwierdzone zmiany. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    public BigDecimal readBalanceUncommitted(long walletId) {
        return balanceOf(walletId);
    }

    private BigDecimal balanceOf(long walletId) {
        return jdbc.sql("SELECT balance FROM wallet WHERE id = ?").param(walletId).query(BigDecimal.class).single();
    }
}
