package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test05TestConfiguration: {@code @TestBean} (Spring Framework 6.2+) — ZASTĘPUJE istniejący bean
 * {@code clock} obiektem z metody fabrycznej testu. W kontekście jest wtedy JEDEN zegar, więc {@code @Primary} nie
 * jest potrzebne. To samo, co {@code @MockitoBean}, ale z prawdziwym obiektem zamiast atrapy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test05")
class Test05TestBeanTest {

    @TestBean // pole "clock" → zastępuje bean typu Clock; obiekt daje statyczna metoda o tej samej nazwie
    Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-11-29T23:30:00Z"), ZoneOffset.UTC); // ostatnie pół godziny promocji
    }

    @Autowired
    PromotionService promotions;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 1: @TestBean zastępuje bean 'clock' — jeden zegar w kontekście, 29 listopada 23:30 to jeszcze promocja")
    void testBeanReplacesClock() {
        assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
        assertThat(promotions.clock()).isSameAs(clock);
        assertThat(promotions.isBlackWeek()).isTrue();
    }
}
