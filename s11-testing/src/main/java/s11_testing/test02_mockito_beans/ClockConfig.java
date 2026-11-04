package s11_testing.test02_mockito_beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Zegar jako bean — test podstawia {@code Clock.fixed(...)} (s11_testing/Test01TestSlices, sekcja 1). */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
