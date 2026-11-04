package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ĆWICZENIA lekcji Tx02Propagation (testy: Tx02PropagationExercisesTest — uruchamiasz sam ▶). Zastąp każde
 * {@code throw} własnym kodem. Ćwiczenia 1 i 3 są w ExerciseAuditLog, część ćwiczenia 4 — w ExerciseOrderWriter.
 * Dane przed każdym testem: Klawiatura 5, Mysz 2, brak zamówień i wpisów audytu.
 */
@Service
public class PropagationExercises {

    private final JdbcClient jdbc;
    private final StockService stock;
    private final ExerciseOrderWriter writer;

    public PropagationExercises(JdbcClient jdbc, StockService stock, ExerciseOrderWriter writer) {
        this.jdbc = jdbc;
        this.stock = stock;
        this.writer = writer;
    }

    /**
     * ĆWICZENIE 2 (średnie): wstaw zamówienie {@code orderNumber} (status NEW), spróbuj zarezerwować towar przez
     * {@link StockService} i ustaw status RESERVED albo — gdy towaru brak — WAITING. Zwróć status. Zamówienie WAITING
     * też ma zostać ZAPISANE (metoda nie może skończyć się wyjątkiem).
     * Uwaga: {@code try { stock.reserve(...) } catch (OutOfStockException e) { ... }} skończy się
     * UnexpectedRollbackException — sprawdź sam, a potem popraw. Podpowiedź: sekcja 2.
     */
    @Transactional
    public String placeOrderOrWait(String orderNumber, String product, int quantity) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, część 2): zaimportuj zamówienia po kolei przez {@link ExerciseOrderWriter#insertImported}.
     * Duplikaty pomiń (złap {@code DuplicateKeyException} — z org.springframework.dao). Pusty numer (blank) przerywa
     * import wyjątkiem {@code IllegalArgumentException}, ale zamówienia zaimportowane PRZED nim mają zostać w bazie.
     * Zwróć liczbę zaimportowanych. {@code @Transactional} na tej metodzie zostaw (np. import zapisuje kiedyś
     * podsumowanie) — Twoja zmiana dotyczy ExerciseOrderWriter.
     * Podpowiedź: sekcje 2 i 3 — czemu zwykły insert w tej transakcji nie wystarczy przy wyjątku na końcu?
     */
    @Transactional
    public int importOrders(List<String> orderNumbers) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
