package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 1–2 lekcji Test05TestConfiguration: ZAGNIEŻDŻONA statyczna klasa {@code @TestConfiguration} jest dołączana
 * AUTOMATYCZNIE — oprócz konfiguracji aplikacji (Test05Application), nie zamiast niej. Dodaje zegar zatrzymany
 * w sobotę 28.11.2026 — w środku czarnego weekendu. Czas w teście jest więc zawsze ten sam (deterministyczny).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Test05TestConfigurationNestedTest {

    @TestConfiguration
    static class BlackWeekendClock {

        @Bean
        @Primary   // w kontekście są DWA zegary: systemowy „clock” i ten; wstrzykiwany jest ten
        Clock blackWeekendClock() {
            return Clock.fixed(Instant.parse("2026-11-28T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        }
    }

    @Autowired
    ApplicationContext context;

    @Autowired
    PromotionService promotions;

    @Autowired
    Clock clock;

    @Test
    @DisplayName("1. Zagnieżdżona @TestConfiguration dołączona sama: dwa zegary w kontekście, wstrzykiwany ten z @Primary")
    void nestedTestConfigurationIsAddedAutomatically() {
        assertThat(context.getBeanNamesForType(Clock.class))
                .containsExactlyInAnyOrder("clock", "blackWeekendClock");
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    @DisplayName("2. Stały zegar: „dziś” jest 28.11.2026 → promocja trwa, 64.99 zł → 51.99 zł (−20%)")
    void fixedClockMakesTimeDeterministic() {
        assertThat(promotions.isBlackWeekend()).isTrue();
        assertThat(promotions.priceFor(new BigDecimal("64.99"))).isEqualByComparingTo("51.99");
    }
}
