package s11_testing.test01_test_slices;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Zegar jako bean (clock = zegar): serwis pyta o czas ten bean, a nie {@code Instant.now()}. Dzięki temu test
 * podstawia {@code Clock.fixed(...)} i zna czas co do sekundy.
 * <p>
 * Klasy {@code @Configuration} NIE trafiają do wycinków {@code @WebMvcTest}/{@code @DataJpaTest} przez skanowanie.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
