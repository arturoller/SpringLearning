package s16_cache_scheduling_async.async05_virtual_threads;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/** Metoda @Async, która mówi, na jakim wątku się wykonała. */
@Service
public class ThreadProbe {

    @Async
    public CompletableFuture<ThreadInfo> whereAmI() {
        return CompletableFuture.completedFuture(ThreadInfo.ofCurrent());
    }
}
