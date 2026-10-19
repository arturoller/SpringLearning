package s16_cache_scheduling_async.sched03_scheduled;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Klasa startowa lekcji Sched03Scheduled. {@code @EnableScheduling} (włącz planowanie zadań) uruchamia przetwarzanie
 * adnotacji {@code @Scheduled}. Zadania mają długie opóźnienie początkowe (1 godzina), więc w testach nie ruszają same.
 * Uruchom ▶ obok main — aplikacja poczeka w tle; zadania wystartują dopiero po godzinie (albo o 8:00 w dni robocze).
 */
@SpringBootApplication
@EnableScheduling
public class Sched03Application {

    /** Zegar jako bean (CLAUDE.md sekcja 5: czas wstrzykujemy, nie wołamy LocalDate.now() w logice). */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(Sched03Application.class, args);
    }
}
