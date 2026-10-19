package s16_cache_scheduling_async.async04_async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Obsługa wyjątków z metod {@code @Async void} (uncaught exception handler = obsługa nieprzechwyconych wyjątków).
 * Metoda void nie ma jak zwrócić wyjątku wywołującemu, bo wywołujący dawno poszedł dalej — dlatego Spring woła tę klasę.
 * Zamiast tylko logować, zbieramy komunikaty do kolejki, żeby test mógł je odebrać bez czekania na zegar.
 */
@Component
public class CollectingExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final BlockingQueue<String> failures = new LinkedBlockingQueue<>();

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        failures.add(method.getName() + ": " + ex.getMessage());
    }

    public BlockingQueue<String> failures() {
        return failures;
    }
}
