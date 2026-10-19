package s16_cache_scheduling_async.solutions.cache02_cache_management;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

/** Rozwiązania wzorcowe ćwiczeń 1–2 lekcji Cache02CacheManagement — zajrzyj dopiero po własnej próbie! */
public final class Cache02CacheManagementSolutions {

    private Cache02CacheManagementSolutions() {
    }

    static boolean solution1(CacheManager cacheManager, String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        return cache != null && cache.get(key) != null; // ValueWrapper != null = wpis istnieje (także z wartością null)
    }

    static int solution2(CacheManager cacheManager, String cacheName) {
        if (cacheManager.getCache(cacheName) instanceof ConcurrentMapCache cache) {
            int size = cache.getNativeCache().size();
            cache.clear();
            return size;
        }
        return 0;
    }
}
