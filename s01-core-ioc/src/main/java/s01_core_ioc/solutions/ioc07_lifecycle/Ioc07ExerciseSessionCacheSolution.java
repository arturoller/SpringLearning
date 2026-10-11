package s01_core_ioc.solutions.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.HashMap;
import java.util.Map;

/** Rozwiązanie ćwiczenia 2 lekcji Ioc07Lifecycle. */
public class Ioc07ExerciseSessionCacheSolution {

    private final Map<String, Integer> prices = new HashMap<>();
    private boolean closed;

    @PostConstruct
    void load() {
        prices.put("Kawa", 2999);
        prices.put("Herbata", 1599);
        prices.put("Czekolada", 899);
    }

    @PreDestroy
    void clear() {
        prices.clear();
        closed = true;
    }

    public int size() {
        return prices.size();
    }

    public boolean isClosed() {
        return closed;
    }
}
