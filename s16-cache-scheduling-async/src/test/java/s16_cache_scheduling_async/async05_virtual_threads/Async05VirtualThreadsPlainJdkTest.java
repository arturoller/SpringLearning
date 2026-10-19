package s16_cache_scheduling_async.async05_virtual_threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Zwykłe testy jednostkowe (bez Springa — najwęższy wycinek): wątki wirtualne to funkcja JDK, nie Springa.
 * Czas nigdzie nie jest asercją; limity czasu (10 s / 30 s) to wyłącznie bezpieczniki przed zawieszeniem testu.
 */
class Async05VirtualThreadsPlainJdkTest {

    @Test
    @DisplayName("6. Wątek wirtualny to nie wątek platformowy: isVirtual(), demon, jedno zadanie = jeden wątek")
    void virtualThreadBasics() throws Exception {
        Thread platform = Thread.ofPlatform().name("platformowy").unstarted(() -> { });
        Thread virtual = Thread.ofVirtual().name("wirtualny").unstarted(() -> { });

        assertThat(platform.isVirtual()).isFalse();
        assertThat(virtual.isVirtual()).isTrue();
        assertThat(virtual.isDaemon()).as("wątki wirtualne są zawsze demonami — nie utrzymują JVM przy życiu").isTrue();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) { // nowy wątek wirtualny na zadanie
            Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());
            assertThat(isVirtual.get(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("7. 10 000 zadań czeka NARAZ — z 8 wątkami platformowymi byłby zastój, z wirtualnymi działa")
    void tenThousandBlockedTasks() throws Exception {
        int tasks = 10_000;
        CountDownLatch allStarted = new CountDownLatch(tasks); // każde zadanie zgłasza start...
        CountDownLatch release = new CountDownLatch(1);        // ...i czeka (blokuje się) na wspólne zwolnienie
        AtomicInteger finished = new AtomicInteger();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> {
                    allStarted.countDown();
                    release.await(); // blokada: wątek wirtualny „odkłada się”, nośnik wraca do pracy
                    return finished.incrementAndGet();
                });
            }
            // Dopiero gdy WSZYSTKIE 10 000 zadań jednocześnie czeka, zwalniamy je. Pula 8 wątków platformowych nigdy
            // nie doszłaby do tego miejsca (osiem zadań zablokowałoby ją na zawsze), więc test nie miałby końca.
            assertThat(allStarted.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(finished.get()).isZero();
            release.countDown();
        } // close() czeka na zakończenie wszystkich zadań

        assertThat(finished.get()).isEqualTo(tasks);
    }

    @Test
    @DisplayName("8. PUŁAPKA: ograniczony zasób (pula połączeń) nadal ogranicza współbieżność, mimo wielu wątków wirtualnych")
    void limitedResourceStillLimits() throws Exception {
        int connections = 10;                              // jak domyślna pula połączeń z bazą (10)
        Semaphore pool = new Semaphore(connections);
        AtomicInteger inUse = new AtomicInteger();
        AtomicInteger maxInUse = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 500; i++) {
                futures.add(executor.submit(() -> {
                    pool.acquire();                       // „pożycz połączenie” — nadmiarowi czekają
                    try {
                        maxInUse.accumulateAndGet(inUse.incrementAndGet(), Math::max);
                        Thread.yield();                   // chwila „pracy”
                        inUse.decrementAndGet();
                    } finally {
                        pool.release();
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(maxInUse.get()).as("500 wątków wirtualnych, ale naraz pracuje najwyżej 10").isLessThanOrEqualTo(connections);
        assertThat(pool.availablePermits()).isEqualTo(connections);
    }
}
