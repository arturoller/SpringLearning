package s17_events.evt03_async_and_outbox;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Asynchroniczni słuchacze ({@code @Async} = wykonaj w osobnym wątku, z puli zadań Springa). */
@Component
public class AsyncListeners {

    private final EventLog log;
    private final BlockingGate gate;
    private final OrderRepository repository;

    public AsyncListeners(EventLog log, BlockingGate gate, OrderRepository repository) {
        this.log = log;
        this.gate = gate;
        this.repository = repository;
    }

    /** Czeka na bramkę — publikujący nie może na niego czekać, więc wraca od razu. */
    @Async
    @EventListener
    public void onGated(GatedEvent event) throws InterruptedException {
        gate.awaitOpen();
        log.add("async:" + event.name() + " (transakcja aktywna=" + TransactionSynchronizationManager.isActualTransactionActive() + ")");
    }

    /** Rzuca wyjątek w osobnym wątku: publikujący go nie zobaczy. */
    @Async
    @EventListener
    public void onFailing(FailingAsyncEvent event) {
        throw new IllegalStateException("Asynchroniczny słuchacz zawiódł: " + event.reason());
    }

    /** Asynchronicznie PO commicie: zamówienie jest już w bazie, a słuchacz działa w osobnym wątku, bez transakcji. */
    @Async
    @TransactionalEventListener
    public void afterCommitAsync(OrderPlacedAsync event) {
        log.add("async-po-commicie:" + event.orderId() + " (zamówień w bazie=" + repository.countOrders()
                + ", transakcja aktywna=" + TransactionSynchronizationManager.isActualTransactionActive() + ")");
    }
}
