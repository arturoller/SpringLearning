package s16_cache_scheduling_async.async04_async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Async04Async. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Async04AsyncExercisesTest {

    @Autowired
    ExerciseNotifier notifier;

    @Test
    @DisplayName("Ćwiczenie 1: suma policzona na podanym wykonawcy")
    void exercise1() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "moja-pula"));
        try {
            CompletableFuture<Integer> future = Async04Async.exercise1(executor, 2, 3);
            assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo(5);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: połączenie dwóch future bez blokowania i bez gubienia błędu")
    void exercise2() throws Exception {
        CompletableFuture<Integer> a = new CompletableFuture<>();
        CompletableFuture<Integer> b = new CompletableFuture<>();
        CompletableFuture<Integer> sum = Async04Async.exercise2(a, b);

        assertThat(sum).as("nic jeszcze nie policzone, a metoda nie może blokować").isNotDone();
        a.complete(4);
        b.complete(6);
        assertThat(sum.get(10, TimeUnit.SECONDS)).isEqualTo(10);

        CompletableFuture<Integer> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("awaria"));
        CompletableFuture<Integer> broken = Async04Async.exercise2(CompletableFuture.completedFuture(1), failed);
        assertThatThrownBy(() -> broken.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: notifyCustomer ma działać w wątku puli Boota")
    void exercise3() throws Exception {
        String worker = notifier.notifyCustomer("jan@example.com").get(10, TimeUnit.SECONDS);

        assertThat(worker).startsWith("task-").isNotEqualTo(Thread.currentThread().getName());
    }
}
