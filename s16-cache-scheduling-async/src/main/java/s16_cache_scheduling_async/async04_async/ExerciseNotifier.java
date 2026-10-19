package s16_cache_scheduling_async.async04_async;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * SZKIELET do ĆWICZENIA 3 lekcji Async04Async. Lekcja NIE używa tej klasy w swoich testach — dzięki temu po
 * rozwiązaniu ćwiczenia zwykłe {@code mvnw test} zostaje zielone.
 */
@Service
public class ExerciseNotifier {

    // TODO (ćwiczenie 3): sprawić, żeby ta metoda wykonywała się w wątku puli Boota (nazwa wątku zaczyna się od "task-"),
    //   a nie w wątku wywołującym. Zmień TYLKO to, co jest konieczne.
    public CompletableFuture<String> notifyCustomer(String email) {
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }
}
