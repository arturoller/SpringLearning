package s10_transactions.tx04_read_only_isolation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ĆWICZENIA lekcji Tx04ReadOnlyIsolation (testy: Tx04ReadOnlyIsolationExercisesTest — uruchamiasz sam ▶). Zastąp każde
 * {@code throw} własnym kodem (adnotacje {@code @Transactional} dopisz sam tam, gdzie są potrzebne).
 * Dane przed każdym testem: portfel 1 Anna Nowak 100.00, portfel 2 Jan Kowalski 50.00 (w tabelach wallet
 * i versioned_wallet; wersja 0).
 */
@Service
public class ReadOnlyIsolationExercises {

    private final WalletRepository wallets;
    private final TopUpService topUps;
    private final JdbcClient jdbc;

    public ReadOnlyIsolationExercises(WalletRepository wallets, TopUpService topUps, JdbcClient jdbc) {
        this.wallets = wallets;
        this.topUps = topUps;
        this.jdbc = jdbc;
    }

    /**
     * ĆWICZENIE 1 (łatwe): „podgląd” zmiany właściciela — wczytaj portfel, zmień właściciela ({@code rename}) i zwróć
     * {@code new RenamePreview(wallet.getOwner(), TransactionSynchronizationManager.isCurrentTransactionReadOnly())}.
     * W bazie NIC nie może się zmienić, a podgląd ma działać w transakcji tylko do odczytu.
     * Podpowiedź: sekcja 1 — jeden atrybut adnotacji.
     */
    public RenamePreview previewRename(long walletId, String newOwner) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wypłata z blokadą pesymistyczną — wczytaj portfel przez
     * {@link WalletRepository#findForUpdateById}, gdy saldo jest mniejsze niż {@code amount} rzuć
     * {@code IllegalStateException}, w przeciwnym razie wypłać ({@code withdraw}) i zwróć nowe saldo.
     * Test sprawdzi, że SELECT miał {@code for update}, a zmiana została zapisana.
     * Podpowiedź: sekcja 4 — bez transakcji ta metoda repozytorium rzuca wyjątek.
     */
    public BigDecimal withdrawWithLock(long walletId, BigDecimal amount) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): przeczytaj saldo portfela JdbcClientem ({@code SELECT balance FROM wallet WHERE id = ?}),
     * wywołaj {@code meanwhile.run()} (w teście: ktoś inny dodaje 50 i zatwierdza), przeczytaj saldo jeszcze raz.
     * Zwróć {@code new TwoReads(pierwszy, drugi, poziom)}, gdzie poziom to wynik zapytania
     * {@code SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()}.
     * Oba odczyty mają dać TĘ SAMĄ wartość.
     * Podpowiedź: sekcja 2 — który poziom izolacji zapobiega non-repeatable read?
     */
    public TwoReads readBalanceTwiceConsistently(long walletId, Runnable meanwhile) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): doładuj portfel z wersją przez {@link TopUpService#topUpVersioned}. Przy pierwszej
     * próbie przekaż {@code meanwhileOnFirstAttempt} (w teście: ktoś inny doładowuje ten sam portfel). Gdy próba
     * skończy się {@code ObjectOptimisticLockingFailureException} (z org.springframework.orm), spróbuj jeszcze raz
     * — już z pustym {@code () -> { }}. Najwyżej 3 próby. Zwróć liczbę wykonanych prób.
     * Wynik w teście: obie wpłaty zapisane (100 + 50 + 30 = 180.00), 2 próby.
     * Podpowiedź: sekcja 3, PUŁAPKA o ponawianiu — czy ta metoda powinna mieć {@code @Transactional}?
     */
    public int topUpWithRetry(long walletId, BigDecimal amount, Runnable meanwhileOnFirstAttempt) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /** Wynik ćwiczenia 1. */
    public record RenamePreview(String owner, boolean readOnlyTransaction) {
    }

    /** Wynik ćwiczenia 3. */
    public record TwoReads(BigDecimal first, BigDecimal second, String isolationLevel) {
    }
}
