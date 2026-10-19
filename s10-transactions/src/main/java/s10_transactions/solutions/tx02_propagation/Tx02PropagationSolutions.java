package s10_transactions.solutions.tx02_propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import s10_transactions.tx02_propagation.AuditEntry;
import s10_transactions.tx02_propagation.AuditEntryRepository;
import s10_transactions.tx02_propagation.AuditService;
import s10_transactions.tx02_propagation.ShopOrder;
import s10_transactions.tx02_propagation.ShopOrderRepository;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Tx02Propagation — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu lekcji — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)}.
 */
@Service
public class Tx02PropagationSolutions {

    private final ShopOrderRepository orders;
    private final AuditEntryRepository auditLog;
    private final AuditService audit;

    public Tx02PropagationSolutions(ShopOrderRepository orders, AuditEntryRepository auditLog, AuditService audit) {
        this.orders = orders;
        this.auditLog = auditLog;
        this.audit = audit;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvenIfCallerFails(String message) {
        auditLog.save(new AuditEntry(message));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelOrderInsideTransaction(long orderId) {
        orders.deleteById(orderId);
    }

    @Transactional
    public void placeOrderWithSafeAudit(String product, int quantity, String auditMessage) {
        orders.save(new ShopOrder(product, quantity));
        try {
            audit.logRequiresNew(auditMessage);      // własna transakcja: jej rollback nie dotyka zamówienia
        } catch (RuntimeException auditFailed) {
            // audyt się nie udał — zamówienie i tak zapisujemy (w prawdziwej aplikacji: zaloguj ostrzeżenie)
        }
    }
}
