package s01_core_ioc.solutions.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

/** Rozwiązanie ćwiczenia 1 lekcji Ioc07Lifecycle. */
public class Ioc07ExerciseCacheSolution {

    private final Map<String, Integer> prices = new HashMap<>();

    @PostConstruct
    void load() {
        prices.put("Kawa", 2999);
        prices.put("Herbata", 1599);
        prices.put("Czekolada", 899);
    }

    public int size() {
        return prices.size();
    }
}
