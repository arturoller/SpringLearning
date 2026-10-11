package s01_core_ioc.ioc06_scopes;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bezpieczna wersja licznika: stan trzyma {@code AtomicInteger} (atomowa liczba całkowita), którego
 * {@code incrementAndGet()} jest niepodzielne, więc żadne zwiększenie nie ginie, nawet przy wielu wątkach.
 */
@Component
public class SafeVisitCounter {

    private final AtomicInteger count = new AtomicInteger();

    public void increment() {
        count.incrementAndGet();
    }

    public int count() {
        return count.get();
    }
}
