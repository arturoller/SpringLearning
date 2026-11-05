package s10_transactions.tx02_propagation;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Stan transakcji w bieżącym wątku (dowód w testach). {@code TransactionSynchronizationManager} — menedżer
 * synchronizacji transakcji; trzyma stan w zmiennych ThreadLocal (osobno dla każdego wątku).
 *
 * @param active czy trwa prawdziwa transakcja (actual transaction active = rzeczywista transakcja aktywna)
 * @param name   nazwa transakcji — dla {@code @Transactional} „pakiet.Klasa.metoda”; bez transakcji zwykle null
 */
public record TxState(boolean active, String name) {

    public static TxState now() {
        return new TxState(TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.getCurrentTransactionName());
    }
}
