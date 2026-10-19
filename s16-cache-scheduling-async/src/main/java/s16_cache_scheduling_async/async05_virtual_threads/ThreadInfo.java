package s16_cache_scheduling_async.async05_virtual_threads;

/** Opis wątku: nazwa i to, czy jest wirtualny (virtual = wirtualny, platform = platformowy, czyli „zwykły” wątek systemowy). */
public record ThreadInfo(String name, boolean virtual) {

    public static ThreadInfo ofCurrent() {
        Thread thread = Thread.currentThread();
        return new ThreadInfo(thread.getName(), thread.isVirtual());
    }
}
