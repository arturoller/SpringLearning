package s10_transactions.tx03_proxy_pitfalls;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * StockWarmup = „rozgrzewka” magazynu przy starcie aplikacji. Pokazuje PUŁAPKĘ: {@code @PostConstruct} (po
 * skonstruowaniu) woła własną metodę {@code @Transactional} — bez transakcji. Zapamiętuje obserwacje w polach,
 * żeby test mógł je odczytać.
 */
@Component
public class StockWarmup {

    private final StockRestocker restocker;
    private boolean transactionInPostConstruct;
    private boolean transactionViaOtherBeanInPostConstruct;
    private boolean transactionOnApplicationReady;

    public StockWarmup(StockRestocker restocker) {
        this.restocker = restocker;
    }

    @PostConstruct
    void init() {
        transactionInPostConstruct = warmUp();                     // this.warmUp() — omija proxy
        transactionViaOtherBeanInPostConstruct = restocker.restock("Kawa", 0);   // inny bean — przez jego proxy
    }

    /** Wywoływane, gdy aplikacja jest w pełni gotowa (ApplicationReadyEvent = zdarzenie „aplikacja gotowa”). */
    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        transactionOnApplicationReady = restocker.restock("Kawa", 0);
    }

    @Transactional
    public boolean warmUp() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    public boolean wasTransactionInPostConstruct() {
        return transactionInPostConstruct;
    }

    public boolean wasTransactionViaOtherBeanInPostConstruct() {
        return transactionViaOtherBeanInPostConstruct;
    }

    public boolean wasTransactionOnApplicationReady() {
        return transactionOnApplicationReady;
    }
}
