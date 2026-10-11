package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * StockService = serwis magazynu. Zbiór PUŁAPEK proxy transakcyjnego z lekcji Tx03ProxyPitfalls i trzech sposobów
 * naprawy. Metody zwracają {@code isActualTransactionActive()} z wnętrza — żeby test mógł sprawdzić, czy transakcja
 * naprawdę była.
 */
@Service
public class StockService {

    private final StockItemRepository stock;
    private final StockRestocker restocker;
    private final StockService self;                       // NAPRAWA nr 2a: @Lazy — „ja sam”, ale przez proxy
    private final ObjectProvider<StockService> selfProvider;  // NAPRAWA nr 2b: dostawca „mnie samego”
    private final TransactionTemplate transactionTemplate;    // NAPRAWA nr 3: transakcja programowa

    public StockService(StockItemRepository stock, StockRestocker restocker, @Lazy StockService self,
                        ObjectProvider<StockService> selfProvider, TransactionTemplate transactionTemplate) {
        this.stock = stock;
        this.restocker = restocker;
        this.self = self;
        this.selfProvider = selfProvider;
        this.transactionTemplate = transactionTemplate;
    }

    // --- 1. Wywołanie przez proxy vs this.metoda() ---------------------------------------------------------------

    /**
     * Przyjmij dostawę. Bez save(): liczymy na dirty checking, który działa TYLKO w transakcji. Zwraca, czy transakcja
     * była aktywna.
     */
    @Transactional
    public boolean restock(String product, int pieces) {
        stock.findById(product).orElseThrow().add(pieces);
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * PUŁAPKA: wywołanie z wnętrza tej samej klasy (self-invocation — wywołanie samego siebie). {@code this} to obiekt
     * docelowy, NIE proxy, więc {@code @Transactional} nad restock jest pomijane: brak transakcji i — co gorsza —
     * zmiana ilości po cichu przepada (encja po findById jest odłączona, nikt nie robi UPDATE).
     */
    public boolean restockViaThis(String product, int pieces) {
        return this.restock(product, pieces);
    }

    // --- 2. Metody prywatne, package-private, protected, final ------------------------------------------------

    /** Publiczna metoda bez adnotacji woła PRYWATNĄ metodę z {@code @Transactional} — adnotacja jest martwa. */
    public boolean callPrivateTransactional() {
        return privateTransactional();
    }

    @Transactional
    private boolean privateTransactional() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Metoda package-private (bez modyfikatora) — od Spring 6.0 proxy klasowe (CGLIB) OBSŁUGUJE ją transakcyjnie. */
    @Transactional
    boolean packagePrivateProbe() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Metoda protected — jak wyżej, obsługiwana przez proxy CGLIB. */
    @Transactional
    protected boolean protectedProbe() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Metoda final — CGLIB NIE MOŻE jej nadpisać, więc proxy nie ma jak wstawić transakcji. */
    @Transactional
    public final boolean finalProbe() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * Metoda final, która używa pola. Wywołana na proxy wykonuje się na OBIEKCIE PROXY, a nie na prawdziwym beanie —
     * a pola obiektu proxy są puste (null). Wynik: NullPointerException.
     */
    @Transactional
    public final long finalCountProducts() {
        return stock.count();
    }

    // --- 4. Połknięty wyjątek → commit częściowych zmian -------------------------------------------------------

    /**
     * PUŁAPKA: zarezerwuj (zdejmij ze stanu) wszystkie pozycje, ale błędy „tylko zaloguj”. Wyjątek nie wychodzi
     * z metody, więc proxy widzi normalny powrót → COMMIT: udane pozycje zostają zdjęte, a nieudane nie.
     * Zwraca liczbę zarezerwowanych pozycji.
     */
    @Transactional
    public int reserveIgnoringErrors(Map<String, Integer> items) {
        int reserved = 0;
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            try {
                stock.findById(item.getKey()).orElseThrow().take(item.getValue());
                reserved++;
            } catch (IllegalStateException notEnough) {
                // „tylko logujemy” — i właśnie zatwierdzamy połowę zamówienia
            }
        }
        return reserved;
    }

    // --- 5. NAPRAWY self-invocation ----------------------------------------------------------------------------

    /** NAPRAWA nr 1: metoda w INNYM beanie (StockRestocker) — wywołanie przechodzi przez jego proxy. */
    public boolean restockViaOtherBean(String product, int pieces) {
        return restocker.restock(product, pieces);
    }

    /** NAPRAWA nr 2a: wstrzyknięty „ja sam” z {@code @Lazy} — to proxy, więc transakcja działa. */
    public boolean restockViaLazySelf(String product, int pieces) {
        return self.restock(product, pieces);
    }

    /** NAPRAWA nr 2b: ObjectProvider (dostawca obiektu) — getObject() zwraca proxy tego beana. */
    public boolean restockViaProvider(String product, int pieces) {
        return selfProvider.getObject().restock(product, pieces);
    }

    /** NAPRAWA nr 3: TransactionTemplate — transakcja otwierana w kodzie, bez proxy (lekcja Tx05). */
    public boolean restockViaTemplate(String product, int pieces) {
        Boolean active = transactionTemplate.execute(status -> {
            stock.findById(product).orElseThrow().add(pieces);
            return TransactionSynchronizationManager.isActualTransactionActive();
        });
        return Boolean.TRUE.equals(active);
    }
}
