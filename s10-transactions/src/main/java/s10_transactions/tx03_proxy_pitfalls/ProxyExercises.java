package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * ĆWICZENIA 1 i 4 lekcji Tx03ProxyPitfalls (testy: Tx03ProxyPitfallsExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem.
 */
@Service
public class ProxyExercises {

    private final JdbcClient jdbc;
    private final ExerciseProductService products;

    public ProxyExercises(JdbcClient jdbc, ExerciseProductService products) {
        this.jdbc = jdbc;
        this.products = products;
    }

    /**
     * ĆWICZENIE 1 (łatwe): rozpoznaj rodzaj obiektu: {@code "CGLIB"} dla proxy CGLIB, {@code "JDK"} dla proxy JDK
     * (dynamic proxy), {@code "BRAK"} dla zwykłego obiektu.
     * Podpowiedź: sekcja 1 — {@code org.springframework.aop.support.AopUtils}: isCglibProxy, isJdkDynamicProxy.
     */
    public String describeProxy(Object bean) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zmień nazwę produktu w INNYM wątku (np. {@code CompletableFuture.supplyAsync(...)}),
     * ale tak, żeby w tym wątku zmiana działała W TRANSAKCJI. Zwróć {@link WorkerReport}: nazwę wątku, który zmienił
     * nazwę, i czy była w nim aktywna transakcja ({@code TxState.now().active()} odczytane w trakcie zmiany).
     * Pusta nazwa: wyjątek z wątku ma dotrzeć do wywołującego (jako przyczyna wyjątku z {@code join()}), a nazwa
     * produktu się nie zmienia.
     * Podpowiedź: sekcja 5 — {@code @Transactional} na TEJ metodzie nie pomoże (inny wątek); transakcję trzeba
     * otworzyć w wątku roboczym — wołając TAM metodę transakcyjną innego beana przez proxy:
     * {@code products.renameAndReportState(...)}; nazwa wątku: {@code Thread.currentThread().getName()}.
     */
    public WorkerReport renameInWorkerThread(Long productId, String newName) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /** Wynik ćwiczenia 4. */
    public record WorkerReport(String threadName, boolean transactionActive) {
    }
}
