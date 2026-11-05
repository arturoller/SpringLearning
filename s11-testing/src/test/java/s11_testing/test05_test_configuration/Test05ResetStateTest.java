package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test05TestConfiguration — ALTERNATYWA dla {@code @DirtiesContext}: zamiast wyrzucać cały kontekst,
 * zeruj stan w {@code @BeforeEach}. Kontekst jest JEDEN (z pamięci podręcznej), a testy przechodzą w dowolnej kolejności.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test05")
class Test05ResetStateTest {

    private static ApplicationContext seenContext;

    @Autowired
    ViewCounter counter;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void resetCounter() {
        counter.reset(); // tani reset stanu zamiast drogiego nowego kontekstu
    }

    @Test
    @DisplayName("Sekcja 4: reset w @BeforeEach (1 z 2) — licznik zaczyna od zera, kontekst ten sam co w drugim teście")
    void first() {
        assertSameContextAndFreshCounter();
    }

    @Test
    @DisplayName("Sekcja 4: reset w @BeforeEach (2 z 2) — identyczny test; oba przechodzą w DOWOLNEJ kolejności")
    void second() {
        assertSameContextAndFreshCounter();
    }

    private void assertSameContextAndFreshCounter() {
        if (seenContext == null) {
            seenContext = context;
        }
        assertThat(context).as("ten sam kontekst z pamięci podręcznej").isSameAs(seenContext);
        assertThat(counter.count()).isZero();
        counter.register();
        assertThat(counter.register()).isEqualTo(2);
    }
}
