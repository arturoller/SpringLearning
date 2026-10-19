package s17_events.evt02_transactional_events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Serwis zamówień. Metody {@code @Transactional} (transactional = transakcyjny) zapisują zamówienie i publikują
 * zdarzenie W TRAKCIE transakcji — to właśnie wtedy {@code @TransactionalEventListener} „zapamiętuje” zdarzenie
 * i czeka na wynik transakcji.
 */
@Service
public class OrderService {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;
    private final EventLog log;

    public OrderService(OrderRepository repository, ApplicationEventPublisher publisher, EventLog log) {
        this.repository = repository;
        this.publisher = publisher;
        this.log = log;
    }

    /** Zapis + publikacja w jednej transakcji; metoda kończy się normalnie, więc transakcja zostaje zatwierdzona. */
    @Transactional
    public void placeOrder(String orderId, String customer, BigDecimal totalPln) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new OrderPlaced(orderId, customer, totalPln));
        log.add("serwis:koniec-metody");
    }

    /** To samo, ale po publikacji rzuca wyjątek — transakcja zostaje WYCOFANA (rollback). */
    @Transactional
    public void placeOrderAndFail(String orderId, String customer, BigDecimal totalPln) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new OrderPlaced(orderId, customer, totalPln));
        throw new IllegalStateException("Płatność odrzucona");
    }

    /** Publikacja BEZ transakcji (brak @Transactional) — pułapka z sekcji 3 lekcji. */
    public void publishWithoutTransaction(String orderId, String customer, BigDecimal totalPln) {
        publisher.publishEvent(new OrderPlaced(orderId, customer, totalPln));
    }

    /** Zapis zamówienia i zdarzenie o audycie; {@code failIn} wskazuje fazę, w której słuchacz rzuci wyjątek (sekcja 4). */
    @Transactional
    public void placeOrderWithAudit(String orderId, String customer, BigDecimal totalPln, String failIn) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new AuditRequested(orderId, failIn));
    }

    /** Zapis zamówienia i zdarzenie o punktach obsługiwane przez NAIWNEGO słuchacza (sekcja 5). */
    @Transactional
    public void placeOrderNaivePoints(String orderId, String customer, BigDecimal totalPln, boolean failAfterWrite) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new NaivePointsRequested(orderId, customer, totalPln.intValue() / 10, failAfterWrite));
    }

    /** Zapis zamówienia i zdarzenie o punktach obsługiwane przez POPRAWNEGO słuchacza (REQUIRES_NEW, sekcja 6). */
    @Transactional
    public void placeOrderCorrectPoints(String orderId, String customer, BigDecimal totalPln, boolean failAfterWrite) {
        repository.insertOrder(orderId, customer, totalPln);
        publisher.publishEvent(new CorrectPointsRequested(orderId, customer, totalPln.intValue() / 10, failAfterWrite));
    }
}
