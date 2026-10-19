package s16_cache_scheduling_async.solutions.cache01_cacheable;

import java.util.Map;
import java.util.function.Function;

/** Rozwiązania wzorcowe ćwiczeń 1–2 lekcji Cache01Cacheable — zajrzyj dopiero po własnej próbie! */
public final class Cache01CacheableSolutions {

    private Cache01CacheableSolutions() {
    }

    static String solution1() {
        return "#category + ':' + #page";
    }

    static <K, V> V solution2(Map<K, V> cache, K key, Function<K, V> loader) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        V value = loader.apply(key);
        if (value != null) { // jak unless = "#result == null"
            cache.put(key, value);
        }
        return value;
    }
}
