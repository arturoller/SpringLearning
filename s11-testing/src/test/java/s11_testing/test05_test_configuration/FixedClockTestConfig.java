package s11_testing.test05_test_configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * OSOBNA (nie zagnieżdżona) klasa {@code @TestConfiguration} z zegarem testowym: 25 listopada 2026, 12:00 UTC —
 * w środku promocji Black Week. Skanowanie komponentów jej NIE znajduje (Boot w testach odfiltrowuje klasy
 * {@code @TestConfiguration}), więc test, który jej chce, dopisuje {@code @Import(FixedClockTestConfig.class)}.
 * <p>
 * {@code @Primary} (główny) jest konieczne: w kontekście są wtedy DWA beany {@code Clock} (z {@code ClockConfig}
 * i ten), a bez {@code @Primary} Spring wybrałby inny niż myślisz (sekcja 1 lekcji, Test05PitfallsTest).
 */
@TestConfiguration(proxyBeanMethods = false) // proxyBeanMethods = false: bez podklasy-pośrednika (CGLIB) — tu niepotrzebnej
public class FixedClockTestConfig {

    public static final Instant BLACK_WEEK_NOON = Instant.parse("2026-11-25T12:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(BLACK_WEEK_NOON, ZoneOffset.UTC);
    }
}
