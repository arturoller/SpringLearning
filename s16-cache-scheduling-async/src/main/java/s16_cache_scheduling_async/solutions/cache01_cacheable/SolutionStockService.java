package s16_cache_scheduling_async.solutions.cache01_cacheable;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/** Rozwiązanie ćwiczenia 3: ta sama klasa co ExerciseStockService, tylko z adnotacją. */
@Service
public class SolutionStockService {

    private final AtomicInteger loads = new AtomicInteger();

    public int loads() {
        return loads.get();
    }

    @Cacheable(cacheNames = "exerciseStock", unless = "#result == 0")
    public int stockOf(String sku) {
        loads.incrementAndGet();
        return switch (sku) {
            case "ELE-001" -> 7;
            case "ELE-002" -> 0;
            default -> 1;
        };
    }
}
