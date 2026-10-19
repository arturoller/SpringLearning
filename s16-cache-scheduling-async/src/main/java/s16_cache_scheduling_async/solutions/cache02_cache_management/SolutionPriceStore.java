package s16_cache_scheduling_async.solutions.cache02_cache_management;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Rozwiązanie ćwiczenia 3: ta sama klasa co ExercisePriceStore, tylko z @CachePut. */
@Service
public class SolutionPriceStore {

    private final Map<String, Long> database = new ConcurrentHashMap<>(Map.of("ELE-001", 549999L));
    private final AtomicInteger loads = new AtomicInteger();

    public int loads() {
        return loads.get();
    }

    @Cacheable("exercisePrices")
    public long priceOf(String sku) {
        loads.incrementAndGet();
        return database.get(sku);
    }

    @CachePut(cacheNames = "exercisePrices", key = "#sku")
    public long changePrice(String sku, long newPrice) {
        database.put(sku, newPrice);
        return newPrice;
    }
}
