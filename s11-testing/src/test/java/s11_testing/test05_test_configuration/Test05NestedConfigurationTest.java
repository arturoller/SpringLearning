package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test05TestConfiguration — PUŁAPKA: zagnieżdżona ZWYKŁA {@code @Configuration} (zamiast
 * {@code @TestConfiguration}). Spring traktuje ją jako JEDYNĄ konfigurację testu i w ogóle nie szuka
 * Test05Application — w kontekście jest tylko zegar, a cała aplikacja (serwisy, komponenty) znika.
 * Test przechodzi, bo SPRAWDZA tę pułapkę; prawdziwy test serwisu z takim błędem dostałby
 * {@code NoSuchBeanDefinitionException} przy wstrzykiwaniu PromotionService.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test05")
class Test05NestedConfigurationTest {

    @Configuration(proxyBeanMethods = false) // BŁĄD (celowy): powinno być @TestConfiguration
    static class ClockOnly {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-27T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 1, PUŁAPKA: zagnieżdżona @Configuration ZASTĘPUJE konfigurację aplikacji — nie ma PromotionService ani ShopInfo")
    void nestedConfigurationReplacesApplication() {
        assertThat(context.getBeanNamesForType(Clock.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(PromotionService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ShopInfo.class)).isEmpty();
    }
}
