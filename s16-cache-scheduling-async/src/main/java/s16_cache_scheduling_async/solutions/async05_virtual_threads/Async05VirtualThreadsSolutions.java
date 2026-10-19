package s16_cache_scheduling_async.solutions.async05_virtual_threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Rozwiązania wzorcowe ćwiczeń lekcji Async05VirtualThreads — zajrzyj dopiero po własnej próbie! */
public final class Async05VirtualThreadsSolutions {

    private Async05VirtualThreadsSolutions() {
    }

    static boolean solution1() throws InterruptedException {
        AtomicBoolean seen = new AtomicBoolean();
        Thread thread = Thread.ofVirtual().start(() -> seen.set(Thread.currentThread().isVirtual()));
        thread.join();
        return seen.get();
    }

    static List<Integer> solution2(int n) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int value = i;
                futures.add(executor.submit(() -> value * value));
            }
            List<Integer> result = new ArrayList<>();
            for (Future<Integer> future : futures) {
                result.add(future.get());
            }
            return result;
        }
    }

    static int solution3(int tasks, int permits, Runnable work) throws Exception {
        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger current = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                futures.add(executor.submit(() -> {
                    semaphore.acquire();
                    try {
                        max.accumulateAndGet(current.incrementAndGet(), Math::max);
                        try {
                            work.run();
                        } finally {
                            current.decrementAndGet();
                        }
                    } finally {
                        semaphore.release();
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }
        return max.get();
    }
}
