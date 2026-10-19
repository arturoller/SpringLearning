package s16_cache_scheduling_async.cache02_cache_management;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SZKIELET do ĆWICZENIA 3 lekcji Cache02CacheManagement. Lekcja NIE używa tej klasy w swoich testach — dzięki temu po
 * rozwiązaniu ćwiczenia zwykłe {@code mvnw test} zostaje zielone.
 */
@Service
public class ExercisePriceStore {

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

    // TODO (ćwiczenie 3): dodaj adnotację tak, żeby po changePrice kolejne priceOf zwracało NOWĄ cenę
    //   BEZ ponownego odczytu z „bazy” (czyli loads() nie rośnie). Metoda musi zwracać nową cenę.
    public long changePrice(String sku, long newPrice) {
        database.put(sku, newPrice);
        return newPrice;
    }
}
