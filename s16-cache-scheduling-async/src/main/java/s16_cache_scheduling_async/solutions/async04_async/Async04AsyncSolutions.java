package s16_cache_scheduling_async.solutions.async04_async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Rozwiązania wzorcowe ćwiczeń 1–2 lekcji Async04Async — zajrzyj dopiero po własnej próbie! */
public final class Async04AsyncSolutions {

    private Async04AsyncSolutions() {
    }

    static CompletableFuture<Integer> solution1(Executor executor, int a, int b) {
        return CompletableFuture.supplyAsync(() -> a + b, executor);
    }

    static CompletableFuture<Integer> solution2(CompletableFuture<Integer> first, CompletableFuture<Integer> second) {
        return first.thenCombine(second, Integer::sum); // błąd któregokolwiek z future przechodzi do wyniku
    }
}
