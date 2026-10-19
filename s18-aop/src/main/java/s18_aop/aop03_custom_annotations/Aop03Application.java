package s18_aop.aop03_custom_annotations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Klasa startowa lekcji Aop03CustomAnnotations. Skanuje tylko ten pakiet (aop03_custom_annotations).
 * Bean Clock (zegar) — MeasureTimeAspect pyta go o czas, zamiast wołać System.currentTimeMillis(). Dzięki temu test
 * podmienia zegar ({@code @MockitoBean}) i dostaje zawsze ten sam, przewidywalny wynik pomiaru.
 * Aspekty z ćwiczeń i OrderOnMethodAspect nie mają {@code @Component}, więc nie są skanowane.
 */
@SpringBootApplication
public class Aop03Application {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(Aop03Application.class, args);
    }
}
