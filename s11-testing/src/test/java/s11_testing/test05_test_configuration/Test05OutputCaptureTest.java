package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Test05TestConfiguration: {@code OutputCaptureExtension} (capture = przechwytywanie) — przechwytuje
 * wszystko, co poszło na {@code System.out}/{@code System.err} (także logi), i podaje to testowi jako
 * {@code CapturedOutput}. Tu zwykły test jednostkowy: serwis tworzymy sami z {@code Clock.fixed} — Spring niepotrzebny.
 */
@ExtendWith(OutputCaptureExtension.class)
class Test05OutputCaptureTest {

    @Test
    @DisplayName("Sekcja 5: w czasie Black Week serwis zapisuje w logu komunikat o rabacie — sprawdzamy go przez CapturedOutput")
    void logsDiscount(CapturedOutput output) {
        PromotionService promotions = new PromotionService(Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC));

        assertThat(promotions.finalPrice(new BigDecimal("50.00"))).isEqualByComparingTo("40.00");
        assertThat(output).contains("Promocja Black Week aktywna — rabat 20% dla ceny 50.00");
    }

    @Test
    @DisplayName("Sekcja 5: poza promocją nie ma wpisu w logu")
    void noLogOutsidePromotion(CapturedOutput output) {
        PromotionService promotions = new PromotionService(Clock.fixed(Instant.parse("2026-12-01T08:00:00Z"), ZoneOffset.UTC));

        assertThat(promotions.finalPrice(new BigDecimal("50.00"))).isEqualByComparingTo("50.00");
        assertThat(output).doesNotContain("Promocja Black Week");
    }
}
