package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test05TestConfiguration: OSOBNA klasa {@code @TestConfiguration} ({@link FixedClockTestConfig})
 * dołączona przez {@code @Import} — tę samą klasę może importować wiele testów. Bez {@code @Import} nie działa
 * (dowód: Test05PitfallsTest, przypadek NotImportedCase).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test05")
@Import(FixedClockTestConfig.class) // import = dołącz konfigurację
class Test05ImportedTestConfigurationTest {

    @Autowired
    PromotionService promotions;

    @Test
    @DisplayName("Sekcja 1: @Import(FixedClockTestConfig) — 25 listopada 2026 trwa Black Week, Kawa 24.90 kosztuje 19.92")
    void importedClockMakesTimeDeterministic() {
        assertThat(promotions.clock().instant()).isEqualTo(Instant.parse("2026-11-25T12:00:00Z"));
        assertThat(promotions.isBlackWeek()).isTrue();
        assertThat(promotions.finalPrice(new BigDecimal("24.90"))).isEqualByComparingTo("19.92");
    }
}
