package s11_testing.test05_test_configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Zegar aplikacji jako bean (clock = zegar) o nazwie {@code clock}. W aplikacji to zegar systemowy; testy lekcji
 * podmieniają go na {@code Clock.fixed(...)} — na kilka sposobów, z których nie każdy działa (sekcja 1 lekcji).
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
