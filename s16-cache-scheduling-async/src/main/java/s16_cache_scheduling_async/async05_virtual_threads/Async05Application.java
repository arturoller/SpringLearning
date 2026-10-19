package s16_cache_scheduling_async.async05_virtual_threads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

/**
 * Klasa startowa lekcji Async05VirtualThreads. Uruchamiana z main włącza wątki wirtualne
 * ({@code spring.threads.virtual.enabled=true} jako ustawienie domyślne, które można nadpisać). Testy lekcji podają to
 * ustawienie jawnie w {@code @SpringBootTest(properties = ...)}, więc działają tak samo bez względu na main.
 * {@code @EnableAsync} i {@code @EnableScheduling} są po to, by Boot utworzył wykonawcę zadań i harmonogram, które
 * lekcja pokazuje.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Async05Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Async05Application.class);
        application.setDefaultProperties(Map.of("spring.threads.virtual.enabled", "true"));
        application.run(args);
    }
}
