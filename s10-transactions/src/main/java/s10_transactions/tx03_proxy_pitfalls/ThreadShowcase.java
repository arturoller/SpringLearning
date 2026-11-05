package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Sekcja 5 lekcji Tx03ProxyPitfalls: transakcja jest związana z WĄTKIEM (ThreadLocal). Praca zlecona innemu wątkowi
 * ({@code CompletableFuture} — t21_concurrency/Concurrency05CompletableFuture) do niej NIE należy.
 * {@code join()} czeka na wynik — kolejność jest jawna, bez usypiania wątków.
 */
@Service
public class ThreadShowcase {

    private final JdbcClient jdbc;

    public ThreadShowcase(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Stan transakcji w wątku wywołującym i w wątku z puli (ForkJoinPool.commonPool) — test. */
    @Transactional
    public TxState[] statesInBothThreads() {
        TxState here = TxState.now();
        TxState there = CompletableFuture.supplyAsync(TxState::now).join();
        return new TxState[] {here, there};
    }

    /**
     * INSERT w wątku z transakcją i INSERT w innym wątku, potem wyjątek. Rollback cofa tylko pierwszy — drugi
     * zatwierdził się sam (autocommit połączenia z puli) i ZOSTAJE (test).
     */
    @Transactional
    public void insertInBothThreadsThenFail() {
        jdbc.sql("INSERT INTO product (name, price) VALUES ('Słuchawki', 199.00)").update();
        CompletableFuture.runAsync(() ->
                jdbc.sql("INSERT INTO product (name, price) VALUES ('Kabel', 19.00)").update()).join();
        throw new IllegalStateException("Awaria po zapisach w dwóch wątkach");
    }
}
