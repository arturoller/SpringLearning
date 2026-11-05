package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Składanie zamówień (order = zamówienie) — serwis „zewnętrzny” lekcji Tx02Propagation. Każda metoda otwiera
 * transakcję (REQUIRED) i woła StockService/AuditLog, których metody też są transakcyjne. Propagacja decyduje,
 * czy wewnętrzne wywołanie DOŁĄCZY do tej transakcji, czy dostanie własną.
 */
@Service
public class OrderService {

    private final JdbcClient jdbc;
    private final StockService stock;
    private final AuditLog audit;

    public OrderService(JdbcClient jdbc, StockService stock, AuditLog audit) {
        this.jdbc = jdbc;
        this.stock = stock;
        this.audit = audit;
    }

    /**
     * Sekcje 1 i 3: audyt (REQUIRES_NEW) → zamówienie NEW → rezerwacja (REQUIRED, dołącza) → status RESERVED.
     * Brak towaru: wyjątek z rezerwacji wycofuje CAŁĄ transakcję (zamówienie znika), ale wpis audytu zostaje (testy).
     */
    @Transactional
    public void placeOrder(String orderNumber, String product, int quantity) {
        audit.record("Próba złożenia zamówienia " + orderNumber);
        insertOrder(orderNumber, "NEW");
        stock.reserve(product, quantity);
        updateStatus(orderNumber, "RESERVED");
    }

    /**
     * Sekcja 2, PUŁAPKA: łapiemy wyjątek z WEWNĘTRZNEJ metody transakcyjnej i kończymy normalnie. Ale wyjątek przeszedł
     * już przez proxy {@code StockService.reserve}, które oznaczyło WSPÓLNĄ transakcję jako rollback-only. Commit się
     * nie uda: {@code UnexpectedRollbackException}, zamówienia nie ma (test).
     */
    @Transactional
    public void placeOrderIgnoringStockError(String orderNumber, String product, int quantity) {
        insertOrder(orderNumber, "NEW");
        try {
            stock.reserve(product, quantity);
            updateStatus(orderNumber, "RESERVED");
        } catch (OutOfStockException e) {
            updateStatus(orderNumber, "WAITING"); // „poczekamy na dostawę” — i tak nie zostanie zapisane
        }
    }

    /** Sekcja 2, PO: metoda wewnętrzna zwraca wynik zamiast rzucać wyjątek — commit działa (test). */
    @Transactional
    public String placeOrderOrWait(String orderNumber, String product, int quantity) {
        insertOrder(orderNumber, "NEW");
        String status = stock.tryReserve(product, quantity) ? "RESERVED" : "WAITING";
        updateStatus(orderNumber, status);
        return status;
    }

    /**
     * Sekcja 3: zamówienie wstawione (jeszcze niezatwierdzone), a potem zajrzenie do bazy z NOWEJ transakcji.
     * Zwraca numer sesji H2 transakcji zewnętrznej i widok z wnętrza REQUIRES_NEW.
     */
    @Transactional
    public SuspendedView insertOrderAndLookFromNewTransaction(String orderNumber) {
        insertOrder(orderNumber, "NEW");
        int outerSession = jdbc.sql("SELECT SESSION_ID()").query(Integer.class).single();
        AuditLog.NewTransactionView inner = audit.inspectFromNewTransaction();
        return new SuspendedView(outerSession, inner);
    }

    /** Wynik {@link #insertOrderAndLookFromNewTransaction}. */
    public record SuspendedView(int outerSessionId, AuditLog.NewTransactionView inner) {
    }

    /**
     * Sekcja 3, PUŁAPKA: rezerwacja (blokuje wiersz towaru), a potem korekta TEGO SAMEGO wiersza w REQUIRES_NEW.
     * Nowa transakcja czeka na blokadę transakcji zewnętrznej, a zewnętrzna czeka, aż nowa się skończy — zakleszczenie.
     */
    @Transactional
    public void reserveAndCorrectInNewTransaction(String product) {
        stock.reserve(product, 1);
        stock.correctInNewTransaction(product, 1);
    }

    /**
     * Sekcja 5: wywołanie WŁASNEJ metody z REQUIRES_NEW ({@code this.recordInternally}) omija proxy — wpis audytu
     * powstaje w TEJ SAMEJ transakcji i znika przy rollbacku zamówienia (test).
     */
    @Transactional
    public void placeOrderWithInternalAudit(String orderNumber, String product, int quantity) {
        recordInternally("Próba złożenia zamówienia " + orderNumber); // = this.recordInternally(...) — bez proxy!
        insertOrder(orderNumber, "NEW");
        stock.reserve(product, quantity);
        updateStatus(orderNumber, "RESERVED");
    }

    /** Sekcja 5: z wnętrza „REQUIRES_NEW” wywołanego przez this — nazwa transakcji to nazwa metody ZEWNĘTRZNEJ (test). */
    @Transactional
    public String transactionNameSeenBySelfInvokedRequiresNew() {
        return currentTransactionNameInNewTransaction();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // przez proxy: nowa transakcja; przez this: ignorowane
    public void recordInternally(String message) {
        jdbc.sql("INSERT INTO audit_log (message) VALUES (?)").param(message).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String currentTransactionNameInNewTransaction() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }

    private void insertOrder(String orderNumber, String status) {
        jdbc.sql("INSERT INTO shop_order (order_number, status) VALUES (?, ?)").params(orderNumber, status).update();
    }

    private void updateStatus(String orderNumber, String status) {
        jdbc.sql("UPDATE shop_order SET status = ? WHERE order_number = ?").params(status, orderNumber).update();
    }
}
