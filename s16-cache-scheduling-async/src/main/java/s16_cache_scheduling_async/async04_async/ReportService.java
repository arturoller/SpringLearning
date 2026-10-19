package s16_cache_scheduling_async.async04_async;

import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Serwis raportów z metodami asynchronicznymi. Większość metod zwraca NAZWĘ WĄTKU, w którym się wykonała —
 * dzięki temu test widzi, czy praca poszła do puli (np. "task-1"), czy została w wątku wywołującym (np. "main").
 */
@Service
public class ReportService {

    // ---- 1. @Async + CompletableFuture: poprawny wzorzec -----------------------------------------------------------

    /** Wykonuje się w wątku puli; wywołujący od razu dostaje CompletableFuture (obietnicę wyniku). */
    @Async
    public CompletableFuture<String> buildReport(String name) {
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }

    /** Wyjątek w metodzie zwracającej CompletableFuture trafia do future — nie ginie. */
    @Async
    public CompletableFuture<String> failingReport() {
        throw new IllegalStateException("brak danych do raportu");
    }

    // ---- 2. @Async void: wyjątek trafia do AsyncUncaughtExceptionHandler -------------------------------------------

    @Async
    public void fireAndForget(String message) {
        throw new IllegalStateException(message);
    }

    // ---- 3. Pułapka: zły typ zwracany -------------------------------------------------------------------------------

    /** Typ zwracany inny niż void/Future/CompletableFuture: Spring 7 odrzuca takie wywołanie wyjątkiem. */
    @Async
    public String wrongReturnType() {
        return "wynik, którego nikt nie odbierze";
    }

    // ---- 4. Pułapka: wywołanie z tej samej klasy i metoda prywatna ---------------------------------------------------

    /** Woła {@code this.buildReport} — omija proxy, więc @Async NIE zadziała. */
    public String callFromInside() throws Exception {
        return buildReport("wewnętrzny").get();
    }

    public String callPrivateFromInside() {
        return privateReport();
    }

    @Async
    private String privateReport() { // @Async na metodzie prywatnej: proxy nie może jej przechwycić
        return Thread.currentThread().getName();
    }

    // ---- 5. Pułapka: utrata kontekstu (MDC) w innym wątku -----------------------------------------------------------

    @Async
    public CompletableFuture<String> readRequestId() {
        return CompletableFuture.completedFuture(String.valueOf(MDC.get("requestId")));
    }
}
