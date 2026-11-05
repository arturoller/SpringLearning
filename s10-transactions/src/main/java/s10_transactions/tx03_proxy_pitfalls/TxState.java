package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Stan transakcji w bieżącym wątku — dowód w testach, czy proxy zadziałało.
 * {@code TransactionSynchronizationManager} (menedżer synchronizacji transakcji) trzyma go w zmiennych ThreadLocal.
 *
 * @param active czy trwa prawdziwa transakcja (actual transaction active)
 * @param name   nazwa transakcji („pakiet.Klasa.metoda”) albo null
 */
public record TxState(boolean active, String name) {

    public static final TxState NONE = new TxState(false, null);

    public static TxState now() {
        return new TxState(TransactionSynchronizationManager.isActualTransactionActive(),
                TransactionSynchronizationManager.getCurrentTransactionName());
    }
}
