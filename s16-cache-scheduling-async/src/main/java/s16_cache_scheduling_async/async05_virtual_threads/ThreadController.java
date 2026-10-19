package s16_cache_scheduling_async.async05_virtual_threads;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/** Punkty końcowe (endpoints) pokazujące wątek obsługujący żądanie i wątek metody @Async. */
@RestController
public class ThreadController {

    private final ThreadProbe probe;

    public ThreadController(ThreadProbe probe) {
        this.probe = probe;
    }

    /** Wątek, na którym kontener serwletów (Tomcat) wykonuje kontroler. */
    @GetMapping("/api/thread")
    public ThreadInfo requestThread() {
        return ThreadInfo.ofCurrent();
    }

    /** Wątek metody @Async (wykonawca zadań Boota). */
    @GetMapping("/api/async-thread")
    public CompletableFuture<ThreadInfo> asyncThread() {
        return probe.whereAmI();
    }
}
