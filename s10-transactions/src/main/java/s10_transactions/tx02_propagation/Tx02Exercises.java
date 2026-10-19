package s10_transactions.tx02_propagation;

import org.springframework.stereotype.Service;

/**
 * Ćwiczenia lekcji Tx02Propagation (opis w Javadocu metod). Bean, żeby po dopisaniu {@code @Transactional} Spring
 * owinął go proxy. Dopóki ćwiczenia nie są rozwiązane, metody tylko rzucają UnsupportedOperationException.
 * Testy: Tx02PropagationExercisesTest (uruchamiasz sam ▶).
 */
@Service
public class Tx02Exercises {

    private final ShopOrderRepository orders;
    private final AuditEntryRepository auditLog;
    private final AuditService audit;

    public Tx02Exercises(ShopOrderRepository orders, AuditEntryRepository auditLog, AuditService audit) {
        this.orders = orders;
        this.auditLog = auditLog;
        this.audit = audit;
    }

    /**
     * ĆWICZENIE 1 (łatwe): zapisz wpis audytu {@code new AuditEntry(message)} tak, żeby PRZETRWAŁ, nawet gdy
     * transakcja wołającego zostanie potem wycofana (test woła tę metodę wewnątrz transakcji, którą wycofuje).
     * Podpowiedź: jedna adnotacja z odpowiednią propagacją + auditLog.save(...).
     */
    public void logEvenIfCallerFails(String message) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): usuń zamówienie o podanym id (orders.deleteById). Ta metoda jest „kawałkiem” większej
     * operacji i NIE WOLNO jej wołać bez transakcji: wywołanie bez transakcji ma się skończyć
     * IllegalTransactionStateException, a zamówienie ma zostać w bazie.
     * Podpowiedź: która propagacja rzuca wyjątek, gdy nie ma transakcji wołającego?
     */
    public void cancelOrderInsideTransaction(long orderId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): w jednej transakcji zapisz zamówienie {@code new ShopOrder(product, quantity)},
     * a potem spróbuj zapisać wpis audytu przez AuditService. Audyt może się nie udać (np. {@code auditMessage == null}
     * → kolumna message jest NOT NULL → wyjątek). Wymaganie: błąd audytu NIE może wycofać zamówienia ani skończyć się
     * UnexpectedRollbackException; metoda ma zakończyć się normalnie.
     * Podpowiedź: wybierz metodę AuditService z propagacją, która ma WŁASNĄ transakcję, i złap wyjątek w try/catch.
     * Dlaczego audit.logRequired(...) w try/catch NIE zadziała? (sekcja 3 lekcji)
     */
    public void placeOrderWithSafeAudit(String product, int quantity, String auditMessage) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
