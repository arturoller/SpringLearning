package s10_transactions.tx05_programmatic_transactions;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Stan transakcji w bieżącym wątku (dowód w testach). {@code TransactionSynchronizationManager} — menedżer
 * synchronizacji transakcji; trzyma stan w zmiennych ThreadLocal (osobno dla każdego wątku).
 *
 * @param active   czy trwa prawdziwa transakcja (actual transaction active = rzeczywista transakcja aktywna)
 * @param name     nazwa transakcji (dla TransactionTemplate — ustawiona przez {@code setName}, inaczej null)
 * @param readOnly czy transakcja jest tylko do odczytu
 */
public record TxState(boolean active, String name, boolean readOnly) {

    public static TxState now() {
        return new TxState(TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    }
}
