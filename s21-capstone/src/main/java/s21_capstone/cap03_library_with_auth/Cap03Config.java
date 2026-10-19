package s21_capstone.cap03_library_with_auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import s21_capstone.cap03_library_with_auth.loan.LibraryRules;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Konfiguracja domeny biblioteki: zegar (jak w Cap02Config — z opcjonalnym zegarem demonstracyjnym
 * {@code cap03.clock.fixed}) i włączenie rekordu LibraryRules ({@code @EnableConfigurationProperties}).
 */
@Configuration
@EnableConfigurationProperties(LibraryRules.class)
public class Cap03Config {

    public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Bean
    Clock clock(@Value("${cap03.clock.fixed:}") String fixed) {
        if (fixed.isBlank()) {
            return Clock.system(ZONE);
        }
        return Clock.fixed(LocalDateTime.parse(fixed).atZone(ZONE).toInstant(), ZONE);
    }
}
