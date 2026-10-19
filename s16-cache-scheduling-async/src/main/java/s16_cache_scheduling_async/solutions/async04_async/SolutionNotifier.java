package s16_cache_scheduling_async.solutions.async04_async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/** Rozwiązanie ćwiczenia 3: ta sama klasa co ExerciseNotifier, tylko z @Async. */
@Service
public class SolutionNotifier {

    @Async
    public CompletableFuture<String> notifyCustomer(String email) {
        return CompletableFuture.completedFuture(Thread.currentThread().getName());
    }
}
