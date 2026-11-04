package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test05TestConfiguration: ZAGNIEŻDŻONA statyczna klasa {@code @TestConfiguration} — Spring
 * wykrywa ją sam (bez {@code @Import}) i DOKŁADA jej beany do normalnej konfiguracji aplikacji (Test05Application
 * nadal jest znajdowana i skanowana).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE) // NONE: bez serwera — lekcja nie ma weba
@ActiveProfiles("test05")
class Test05NestedTestConfigurationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class BeforeBlackWeekClock {

        @Bean
        @Primary // w kontekście są dwa zegary — ten ma pierwszeństwo przy wstrzykiwaniu
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-20T09:00:00Z"), ZoneOffset.UTC); // 3 dni przed promocją
        }
    }

    @Autowired
    PromotionService promotions;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 1: zagnieżdżona @TestConfiguration DODAJE zegar testowy — reszta aplikacji (ShopInfo, PriceClient) jest na miejscu")
    void nestedTestConfigurationAddsBeans() {
        assertThat(promotions.isBlackWeek()).isFalse();                                    // 20 listopada — przed promocją
        assertThat(promotions.finalPrice(new BigDecimal("100"))).isEqualByComparingTo("100.00");
        assertThat(context.getBeansOfType(Clock.class)).containsOnlyKeys("clock", "fixedClock"); // DWA zegary: dodany, nie zastąpiony
        assertThat(context.getBeanNamesForType(ShopInfo.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(PriceClient.class)).hasSize(1);
    }
}
