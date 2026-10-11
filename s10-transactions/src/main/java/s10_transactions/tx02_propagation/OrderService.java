package s10_transactions.tx02_propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * OrderService = serwis zamówień. To „transakcja zewnętrzna” (outer): zapisuje zamówienie i woła AuditService
 * (transakcja wewnętrzna, inner). Wynik zależy od propagacji metody audytu — patrz lekcja Tx02Propagation.
 */
@Service
public class OrderService {

    private final ShopOrderRepository orders;
    private final AuditService audit;

    public OrderService(ShopOrderRepository orders, AuditService audit) {
        this.orders = orders;
        this.audit = audit;
    }

    /**
     * Zapisz zamówienie, zapisz wpis audytu wybraną propagacją, a potem zgłoś brak towaru (RuntimeException)
     * → zewnętrzna transakcja jest wycofywana. Pytanie: czy wpis audytu przetrwa?
     */
    @Transactional
    public void placeOrderThenFail(String product, int quantity, Propagation auditPropagation) {
        orders.save(new ShopOrder(product, quantity));
        String message = "Próba zamówienia: " + product;
        switch (auditPropagation) {
            case REQUIRED -> audit.logRequired(message);
            case REQUIRES_NEW -> audit.logRequiresNew(message);
            case NESTED -> audit.logNested(message);
            default -> throw new IllegalArgumentException("Nieobsługiwana propagacja: " + auditPropagation);
        }
        throw new IllegalStateException("Brak towaru: " + product);
    }

    /**
     * Zapisz zamówienie, a audyt (który się wywraca) owiń w try/catch — „audyt nie może zepsuć zamówienia”.
     * Działa albo nie — zależnie od propagacji audytu (sekcje 3 i 4 lekcji).
     */
    @Transactional
    public void placeOrderIgnoringAuditFailure(String product, int quantity, Propagation auditPropagation) {
        orders.save(new ShopOrder(product, quantity));
        String message = "Zamówienie: " + product;
        try {
            switch (auditPropagation) {
                case REQUIRED -> audit.logRequiredAndFail(message);
                case REQUIRES_NEW -> audit.logRequiresNewAndFail(message);
                case NESTED -> audit.logNestedAndFail(message);
                default -> throw new IllegalArgumentException("Nieobsługiwana propagacja: " + auditPropagation);
            }
        } catch (IllegalStateException auditFailed) {
            // „Połykamy” błąd audytu i idziemy dalej — zamówienie ma się zapisać. Czy na pewno się zapisze?
        }
    }

    /** Wywołuje sondy SUPPORTS, NOT_SUPPORTED z WNĘTRZA transakcji: [supports, notSupported]. */
    @Transactional
    public List<Boolean> probesInsideTransaction() {
        return List.of(audit.supportsSeesTransaction(), audit.notSupportedSeesTransaction());
    }

    /** Woła metodę z propagacją NEVER z wnętrza transakcji → IllegalTransactionStateException. */
    @Transactional
    public boolean callNeverInsideTransaction() {
        return audit.neverSeesTransaction();
    }

    /** Woła MANDATORY z wnętrza transakcji — działa, bo transakcja jest. */
    @Transactional
    public void placeOrderWithMandatoryAudit(String product, int quantity) {
        orders.save(new ShopOrder(product, quantity));
        audit.logMandatory("Zamówienie: " + product);
    }

    /** Nazwy transakcji: [ta metoda, audyt REQUIRED, audyt REQUIRES_NEW]. */
    @Transactional
    public List<String> transactionNames() {
        return List.of(TransactionSynchronizationManager.getCurrentTransactionName(),
                audit.transactionNameRequired(),
                audit.transactionNameRequiresNew());
    }
}
