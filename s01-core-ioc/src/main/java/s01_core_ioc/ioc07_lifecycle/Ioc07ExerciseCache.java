package s01_core_ioc.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

/**
 * Szkielet do ĆWICZENIA 1 lekcji Ioc07Lifecycle: cache cen produktów. Klasa NIE ma {@code @Component} — test
 * rejestruje ją ręcznie. Dopóki metoda load() rzuca wyjątek, kontekst w teście się nie uruchomi (test jest czerwony).
 */
public class Ioc07ExerciseCache {

    private final Map<String, Integer> prices = new HashMap<>();

    /**
     * ĆWICZENIE 1 (łatwe): wypełnij mapę prices trzema produktami (np. Kawa 2999, Herbata 1599, Czekolada 899).
     * Podpowiedź: {@code prices.put(nazwa, cenaWGroszach)}. Metoda ma adnotację @PostConstruct, więc Spring wywoła ją
     * sam, tuż po utworzeniu beana — nie wołasz jej ręcznie.
     */
    @PostConstruct
    void load() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    public int size() {
        return prices.size();
    }
}
