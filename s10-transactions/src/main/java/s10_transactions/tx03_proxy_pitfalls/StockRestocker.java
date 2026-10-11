package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * StockRestocker = osobny bean do przyjmowania dostaw. NAPRAWA nr 1 z lekcji: metodę transakcyjną przenosimy do
 * INNEGO beana — wtedy wywołanie z StockService przechodzi przez proxy i transakcja działa.
 */
@Service
public class StockRestocker {

    private final StockItemRepository stock;

    public StockRestocker(StockItemRepository stock) {
        this.stock = stock;
    }

    /** Przyjmij dostawę; zwraca, czy w środku trwała transakcja. */
    @Transactional
    public boolean restock(String product, int pieces) {
        stock.findById(product).orElseThrow().add(pieces);     // dirty checking → UPDATE przy commicie
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
}
