package s10_transactions.solutions.tx03_proxy_pitfalls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;
import s10_transactions.tx03_proxy_pitfalls.StockItem;
import s10_transactions.tx03_proxy_pitfalls.StockItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Tx03ProxyPitfalls — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu lekcji — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)}.
 */
@Service
public class Tx03ProxyPitfallsSolutions {

    static final int MAX_QUANTITY = 1000;

    private final StockItemRepository stock;
    private final TransactionTemplate transactionTemplate;

    public Tx03ProxyPitfallsSolutions(StockItemRepository stock, TransactionTemplate transactionTemplate) {
        this.stock = stock;
        this.transactionTemplate = transactionTemplate;
    }

    /** Jedna transakcja na całą metodę: wyjątek z orElseThrow wylatuje przez proxy → rollback wszystkiego. */
    @Transactional
    public void restockAll(Map<String, Integer> deliveries) {
        deliveries.forEach((sku, pieces) -> stock.findById(sku).orElseThrow().add(pieces));
    }

    /** Wynik zamiast wyjątku: setRollbackOnly() → proxy wycofa transakcję, choć metoda kończy się normalnie. */
    @Transactional
    public boolean reserveAllOrNothing(Map<String, Integer> items) {
        try {
            items.forEach((sku, pieces) -> stock.findById(sku).orElseThrow().take(pieces));
            return true;
        } catch (IllegalStateException notEnough) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * BEZ @Transactional nad metodą: każda dostawa dostaje własną transakcję z TransactionTemplate. Wyjątek z lambdy
     * wylatuje z execute… (szablon robi rollback TEJ dostawy), a łapiemy go dopiero na zewnątrz.
     */
    public List<String> restockEachSeparately(Map<String, Integer> deliveries) {
        List<String> failed = new ArrayList<>();
        deliveries.forEach((sku, pieces) -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    StockItem item = stock.findById(sku).orElseThrow();
                    item.add(pieces);
                    if (item.getQuantity() > MAX_QUANTITY) {
                        throw new IllegalStateException("Limit magazynu przekroczony dla " + sku);
                    }
                });
            } catch (RuntimeException deliveryFailed) {
                failed.add(sku);
            }
        });
        return failed;
    }
}
