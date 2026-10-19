package s20_modulith.mod01_modules.orders.internal;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Szczegół wewnętrzny modułu {@code orders}: nadawanie kolejnych numerów zamówień (ZAM-001, ZAM-002, …).
 * Licznik {@code AtomicInteger} (liczba atomowa — bezpieczna przy wielu wątkach naraz).
 */
@Component
public class OrderNumbers {

    private final AtomicInteger counter = new AtomicInteger();

    public String next() {
        return "ZAM-%03d".formatted(counter.incrementAndGet());
    }
}
