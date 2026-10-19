package s16_cache_scheduling_async.async04_async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ClassUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Async04Async. Asynchroniczność sprawdzamy przez CompletableFuture.get(10, SECONDS) — 10 sekund to tylko
 * zabezpieczenie przed zawieszeniem testu, nigdy asercja na czasie (czekanie kończy się natychmiast po wyniku).
 */
@SpringBootTest
class Async04AsyncTest {

    @Autowired
    ReportService reports;

    @Autowired
    CollectingExceptionHandler exceptionHandler;

    @Autowired
    ThreadPoolTaskExecutor applicationTaskExecutor;

    @AfterEach
    void clearMdc() {
        MDC.clear();
        exceptionHandler.failures().clear();
    }

    @Test
    @DisplayName("1. @Async wykonuje metodę w wątku puli Boota (applicationTaskExecutor, prefiks task-)")
    void asyncRunsOnPoolThread() throws Exception {
        String worker = reports.buildReport("miesięczny").get(10, TimeUnit.SECONDS);

        assertThat(AopUtils.isAopProxy(reports)).isTrue();
        assertThat(worker).startsWith("task-").isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("2. Domyślny wykonawca Boota: ThreadPoolTaskExecutor, rdzeń 8 wątków, nieograniczona kolejka")
    void defaultExecutor() {
        assertThat(applicationTaskExecutor.getCorePoolSize()).isEqualTo(8);
        assertThat(applicationTaskExecutor.getThreadNamePrefix()).isEqualTo("task-");
        // Kolejka nieograniczona → pula NIGDY nie urośnie ponad rdzeń (max-size jest wtedy ignorowany).
        assertThat(applicationTaskExecutor.getQueueCapacity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("3. Wyjątek w metodzie zwracającej CompletableFuture trafia do future")
    void exceptionInFutureMethod() {
        assertThatThrownBy(() -> reports.failingReport().get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("brak danych do raportu");
    }

    @Test
    @DisplayName("4. Wyjątek w metodzie @Async void trafia do AsyncUncaughtExceptionHandler")
    void exceptionInVoidMethod() throws Exception {
        reports.fireAndForget("nie udało się wysłać");

        String failure = exceptionHandler.failures().poll(10, TimeUnit.SECONDS);
        assertThat(failure).isEqualTo("fireAndForget: nie udało się wysłać");
    }

    @Test
    @DisplayName("5. PUŁAPKA: typ zwracany inny niż void/Future — wywołanie kończy się IllegalArgumentException")
    void wrongReturnTypeIsRejected() {
        assertThatThrownBy(() -> reports.wrongReturnType())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid return type for async method (only Future and void supported): class java.lang.String");
    }

    @Test
    @DisplayName("6. PUŁAPKA: wywołanie z tej samej klasy omija proxy — metoda jedzie w wątku wywołującym")
    void selfInvocationIsNotAsync() throws Exception {
        assertThat(reports.callFromInside()).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("7. PUŁAPKA: @Async na metodzie prywatnej nie działa")
    void privateMethodIsNotAsync() {
        assertThat(reports.callPrivateFromInside()).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("8. PUŁAPKA: MDC (ThreadLocal) nie przechodzi do wątku puli")
    void mdcIsLost() throws Exception {
        MDC.put("requestId", "REQ-123");

        assertThat(MDC.get("requestId")).isEqualTo("REQ-123");
        assertThat(reports.readRequestId().get(10, TimeUnit.SECONDS)).as("w wątku puli MDC jest puste").isEqualTo("null");
    }

    @Test
    @DisplayName("9. Boot 4.1: spring.task.execution.propagate-context wymaga biblioteki context-propagation (jej tu nie ma)")
    void propagateContextNeedsLibrary() {
        assertThat(ClassUtils.isPresent("io.micrometer.context.ContextSnapshot", getClass().getClassLoader())).isFalse();
    }
}
