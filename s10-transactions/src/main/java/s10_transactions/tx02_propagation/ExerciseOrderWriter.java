package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Część ĆWICZENIA 4 lekcji Tx02Propagation — zapis jednego zamówienia (testy: Tx02PropagationExercisesTest). */
@Service
public class ExerciseOrderWriter {

    private final JdbcClient jdbc;

    public ExerciseOrderWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * ĆWICZENIE 4 (trudniejsze, część 1): wstaw zamówienie {@code orderNumber} ze statusem {@code IMPORTED}
     * i ZATWIERDŹ je od razu — niezależnie od transakcji, z której wywołano metodę.
     * Duplikat numeru rzuci {@code DuplicateKeyException} (podklasa DataIntegrityViolationException) — niech leci dalej.
     * Podpowiedź: sekcja 3; SQL: {@code INSERT INTO shop_order (order_number, status) VALUES (?, 'IMPORTED')}.
     */
    public void insertImported(String orderNumber) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
