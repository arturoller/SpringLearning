package s16_cache_scheduling_async.solutions.async04_async;

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

/** Sprawdzenia z Async04AsyncExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@SpringBootTest
class Async04AsyncSolutionsTest {

    @Autowired
    SolutionNotifier notifier;

    @Test
    @DisplayName("Wzorzec 1: suma policzona na podanym wykonawcy")
    void solution1() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertThat(Async04AsyncSolutions.solution1(executor, 2, 3).get(10, TimeUnit.SECONDS)).isEqualTo(5);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Wzorzec 2: połączenie dwóch future bez blokowania i bez gubienia błędu")
    void solution2() throws Exception {
        CompletableFuture<Integer> a = new CompletableFuture<>();
        CompletableFuture<Integer> b = new CompletableFuture<>();
        CompletableFuture<Integer> sum = Async04AsyncSolutions.solution2(a, b);

        assertThat(sum).isNotDone();
        a.complete(4);
        b.complete(6);
        assertThat(sum.get(10, TimeUnit.SECONDS)).isEqualTo(10);

        CompletableFuture<Integer> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("awaria"));
        CompletableFuture<Integer> broken = Async04AsyncSolutions.solution2(CompletableFuture.completedFuture(1), failed);
        assertThatThrownBy(() -> broken.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Wzorzec 3: notifyCustomer działa w wątku puli Boota")
    void solution3() throws Exception {
        String worker = notifier.notifyCustomer("jan@example.com").get(10, TimeUnit.SECONDS);

        assertThat(worker).startsWith("task-").isNotEqualTo(Thread.currentThread().getName());
    }
}
