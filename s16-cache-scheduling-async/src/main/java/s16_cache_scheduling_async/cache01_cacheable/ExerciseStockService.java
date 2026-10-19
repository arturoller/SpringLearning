package s16_cache_scheduling_async.cache01_cacheable;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * SZKIELET do ĆWICZENIA 3 lekcji Cache01Cacheable (patrz Cache01Cacheable). Lekcja NIE używa tej klasy w swoich testach —
 * dzięki temu po rozwiązaniu ćwiczenia zwykłe {@code mvnw test} zostaje zielone.
 */
@Service
public class ExerciseStockService {

    private final AtomicInteger loads = new AtomicInteger();

    /** Ile razy „baza” została naprawdę zapytana. */
    public int loads() {
        return loads.get();
    }

    // TODO (ćwiczenie 3): dodaj adnotację tak, żeby wynik był pamiętany w pamięci "exerciseStock" pod kluczem = sku,
    //   ALE wartość 0 (brak towaru) nie mogła trafić do pamięci podręcznej.
    public int stockOf(String sku) {
        loads.incrementAndGet();
        return switch (sku) {
            case "ELE-001" -> 7;
            case "ELE-002" -> 0;
            default -> 1;
        };
    }
}
