package s10_transactions.tx01_transactional;

/**
 * Poważny błąd (ledger corrupted = uszkodzona księga). Dziedziczy po {@code Error} — tak jak
 * {@code OutOfMemoryError}. Sekcja 3 lekcji: {@code Error} domyślnie WYCOFUJE transakcję (tak samo jak RuntimeException).
 * W prawdziwym kodzie nie tworzymy własnych {@code Error} — tu tylko do pokazania reguły w teście.
 */
public class LedgerCorruptedError extends Error {

    public LedgerCorruptedError(String message) {
        super(message);
    }
}
