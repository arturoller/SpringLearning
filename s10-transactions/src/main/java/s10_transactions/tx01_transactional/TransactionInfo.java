package s10_transactions.tx01_transactional;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Migawka stanu transakcji w bieżącym wątku — dowód w testach, czy transakcja naprawdę jest aktywna.
 * {@code TransactionSynchronizationManager} (menedżer synchronizacji transakcji) trzyma ten stan w zmiennych
 * {@code ThreadLocal} (wartość osobna dla każdego wątku — s10_transactions/Tx03ProxyPitfalls, sekcja 5).
 *
 * @param active   czy jest prawdziwa transakcja (actual transaction active = rzeczywista transakcja aktywna)
 * @param name     nazwa transakcji; dla {@code @Transactional} to „pełna.nazwa.Klasy.metoda”, bez transakcji null
 * @param readOnly czy transakcja jest tylko do odczytu (read only)
 */
public record TransactionInfo(boolean active, String name, boolean readOnly) {

    /** Odczyt stanu „tu i teraz” — w wątku, który woła tę metodę. */
    public static TransactionInfo now() {
        return new TransactionInfo(TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    }
}
