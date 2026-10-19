package s16_cache_scheduling_async.async04_async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Klasa startowa lekcji Async04Async. {@code @EnableAsync} (włącz metody asynchroniczne) tworzy proxy, które oddaje
 * wywołanie metod z {@code @Async} do puli wątków. Uruchom ▶ obok main i wysyłaj zapytania z pliku
 * s16-cache-scheduling-async/http/async04_async.http.
 */
@SpringBootApplication
@EnableAsync
public class Async04Application {

    public static void main(String[] args) {
        SpringApplication.run(Async04Application.class, args);
    }
}
