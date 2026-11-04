package s10_transactions.solutions.tx04_read_only_isolation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import s10_transactions.tx04_read_only_isolation.ReadOnlyIsolationExercises.RenamePreview;
import s10_transactions.tx04_read_only_isolation.ReadOnlyIsolationExercises.TwoReads;
import s10_transactions.tx04_read_only_isolation.TopUpService;
import s10_transactions.tx04_read_only_isolation.Wallet;
import s10_transactions.tx04_read_only_isolation.WalletRepository;

import java.math.BigDecimal;

/** Rozwiązania wzorcowe ćwiczeń lekcji Tx04ReadOnlyIsolation (testy: Tx04ReadOnlyIsolationSolutionsTest). */
@Service
public class SolutionReadOnlyIsolationExercises {

    private static final int MAX_ATTEMPTS = 3;

    private final WalletRepository wallets;
    private final TopUpService topUps;
    private final JdbcClient jdbc;

    public SolutionReadOnlyIsolationExercises(WalletRepository wallets, TopUpService topUps, JdbcClient jdbc) {
        this.wallets = wallets;
        this.topUps = topUps;
        this.jdbc = jdbc;
    }

    /** ĆWICZENIE 1: readOnly = true — zmiana zostaje w pamięci, flush się nie odbywa. */
    @Transactional(readOnly = true)
    public RenamePreview previewRename(long walletId, String newOwner) {
        Wallet wallet = wallets.findById(walletId).orElseThrow();
        wallet.rename(newOwner);
        return new RenamePreview(wallet.getOwner(), TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    }

    /** ĆWICZENIE 2: blokada wiersza do końca transakcji — sprawdzenie salda i zapis bez ryzyka lost update. */
    @Transactional
    public BigDecimal withdrawWithLock(long walletId, BigDecimal amount) {
        Wallet wallet = wallets.findForUpdateById(walletId).orElseThrow();
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Za mało środków w portfelu " + walletId);
        }
        wallet.withdraw(amount);
        return wallet.getBalance();
    }

    /** ĆWICZENIE 3: REPEATABLE READ — oba odczyty z tej samej migawki. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TwoReads readBalanceTwiceConsistently(long walletId, Runnable meanwhile) {
        BigDecimal first = balance(walletId);
        meanwhile.run();
        BigDecimal second = balance(walletId);
        String level = jdbc.sql("SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()")
                .query(String.class).single();
        return new TwoReads(first, second, level);
    }

    /**
     * ĆWICZENIE 4: ponawianie POZA transakcją (brak {@code @Transactional} tutaj!) — każda próba to nowe wywołanie przez
     * proxy TopUpService, czyli nowa transakcja i świeży odczyt wersji.
     */
    public int topUpWithRetry(long walletId, BigDecimal amount, Runnable meanwhileOnFirstAttempt) {
        for (int attempt = 1; ; attempt++) {
            try {
                topUps.topUpVersioned(walletId, amount, attempt == 1 ? meanwhileOnFirstAttempt : () -> { });
                return attempt;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private BigDecimal balance(long walletId) {
        return jdbc.sql("SELECT balance FROM wallet WHERE id = ?").param(walletId).query(BigDecimal.class).single();
    }
}
