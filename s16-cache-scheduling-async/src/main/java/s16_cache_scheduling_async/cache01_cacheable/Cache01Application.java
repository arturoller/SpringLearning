package s16_cache_scheduling_async.cache01_cacheable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Klasa startowa lekcji Cache01Cacheable. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s16-cache-scheduling-async/http/cache01_cacheable.http.
 *
 * <p>{@code @EnableCaching} (włącz pamięć podręczną) — BEZ tej adnotacji {@code @Cacheable} jest po cichu ignorowane.</p>
 */
@SpringBootApplication
@EnableCaching
public class Cache01Application {

    public static void main(String[] args) {
        SpringApplication.run(Cache01Application.class, args);
    }
}
