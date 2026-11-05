package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Część ĆWICZEŃ lekcji Tx02Propagation (ćwiczenia 1 i 3) — dziennik i księga zdarzeń. Testy:
 * Tx02PropagationExercisesTest. Zapisuj do tabeli {@code audit_log} (kolumna {@code message}).
 */
@Service
public class ExerciseAuditLog {

    private final JdbcClient jdbc;

    public ExerciseAuditLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zapisz {@code message} do audit_log tak, żeby wpis ZOSTAŁ w bazie nawet wtedy, gdy
     * transakcja wywołującego zostanie potem wycofana.
     * Podpowiedź: sekcja 3 — {@code @Transactional(propagation = Propagation.REQUIRES_NEW)}; SQL:
     * {@code INSERT INTO audit_log (message) VALUES (?)}.
     */
    public void record(String message) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zapisz {@code "KSIĘGA: " + entry} do audit_log, ale TYLKO jako część trwającej transakcji
     * wywołującego (wycofanie wywołującego wycofuje też wpis). Wywołanie bez transakcji ma się skończyć wyjątkiem
     * {@code IllegalTransactionStateException} — zanim cokolwiek zostanie zapisane.
     * Podpowiedź: sekcja 4 — która propagacja „wymaga” istniejącej transakcji?
     */
    public void postToLedger(String entry) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
