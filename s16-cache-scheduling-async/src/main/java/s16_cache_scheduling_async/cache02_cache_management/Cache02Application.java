package s16_cache_scheduling_async.cache02_cache_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Klasa startowa lekcji Cache02CacheManagement. Uruchom ją (▶ obok main), a potem wysyłaj zapytania z pliku
 * s16-cache-scheduling-async/http/cache02_cache_management.http.
 */
@SpringBootApplication
@EnableCaching
public class Cache02Application {

    public static void main(String[] args) {
        SpringApplication.run(Cache02Application.class, args);
    }
}
