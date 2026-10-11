package s01_core_ioc.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.HashMap;
import java.util.Map;

/**
 * Szkielet do ĆWICZENIA 2 lekcji Ioc07Lifecycle. Ładowanie danych jest gotowe, Ty piszesz sprzątanie.
 * Klasa NIE ma {@code @Component} — test rejestruje ją ręcznie.
 */
public class Ioc07ExerciseSessionCache {

    private final Map<String, Integer> prices = new HashMap<>();
    private boolean closed;

    @PostConstruct
    void load() {
        prices.put("Kawa", 2999);
        prices.put("Herbata", 1599);
        prices.put("Czekolada", 899);
    }

    /**
     * ĆWICZENIE 2 (średnie): wyczyść mapę prices i ustaw closed = true. Podpowiedź: {@code prices.clear()}.
     * Metoda ma adnotację @PreDestroy, więc Spring wywoła ją przy zamknięciu kontekstu (context.close()).
     * Uwaga: wyjątek z metody @PreDestroy nie zatrzymuje zamykania — Spring tylko go zaloguje, więc brak
     * czerwonego błędu w kontekście nie znaczy, że zadanie jest zrobione. Patrz na wynik testu.
     */
    @PreDestroy
    void clear() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    public int size() {
        return prices.size();
    }

    public boolean isClosed() {
        return closed;
    }
}
