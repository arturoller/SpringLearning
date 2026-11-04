package s11_testing.solutions.test05_test_configuration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import s11_testing.test05_test_configuration.NewsletterConfig;
import s11_testing.test05_test_configuration.NewsletterSender;
import s11_testing.test05_test_configuration.PriceClient;
import s11_testing.test05_test_configuration.PriceStubServer;
import s11_testing.test05_test_configuration.PromotionService;
import s11_testing.test05_test_configuration.ShopInfo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Rozwiązania wzorcowe ćwiczeń Test05TestConfiguration. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class Test05TestConfigurationSolutionsTest {

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    class Exercise1 {

        @TestConfiguration(proxyBeanMethods = false)
        static class DayAfterClock {
            @Bean
            @Primary
            Clock dayAfterClock() {
                return Clock.fixed(Instant.parse("2026-11-30T00:00:00Z"), ZoneOffset.UTC);
            }
        }

        @Autowired
        PromotionService promotions;

        @Test
        @DisplayName("Wzorzec 1: 30 listopada 00:00 UTC — promocja już się skończyła")
        void promotionOver() {
            assertThat(promotions.clock().instant()).isEqualTo(Instant.parse("2026-11-30T00:00:00Z")); // to NASZ zegar
            assertThat(promotions.isBlackWeek()).isFalse();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "test05.shop.name=Mój sklep")
    @ActiveProfiles("test05")
    class Exercise2 {

        @Autowired
        ShopInfo shop;

        @Test
        @DisplayName("Wzorzec 2: właściwość z @SpringBootTest(properties) wygrywa z profilem")
        void inlineProperty() {
            assertThat(shop.name()).isEqualTo("Mój sklep");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    class Exercise3 {

        static final PriceStubServer STUB = PriceStubServer.start(Map.of("MYSZ", "59.00"));

        @DynamicPropertySource
        static void priceServiceUrl(DynamicPropertyRegistry registry) {
            registry.add("test05.price-service.url", STUB::baseUrl);
        }

        @AfterAll
        static void stop() {
            STUB.close();
        }

        @Autowired
        PriceClient prices;

        @Test
        @DisplayName("Wzorzec 3: adres atrapy z @DynamicPropertySource — MYSZ kosztuje 59.00")
        void priceFromStub() {
            assertThat(prices.currentPrice("MYSZ")).isEqualByComparingTo("59.00");
        }
    }

    @Nested
    class Exercise4 {

        @Test
        @DisplayName("Wzorzec 4: newsletter wyłączony właściwością → wersja 'nic nie rób'")
        void newsletterDisabled() {
            new ApplicationContextRunner()
                    .withUserConfiguration(NewsletterConfig.class)
                    .withPropertyValues("test05.newsletter.enabled=false")
                    .run(context -> {
                        assertThat(context).hasSingleBean(NewsletterSender.class);
                        assertThat(context.getBean(NewsletterSender.class).send("x")).isEqualTo("Newsletter wyłączony");
                    });
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    class Exercise5 {

        @TestConfiguration(proxyBeanMethods = false)
        static class MutableClockConfig {
            @Bean
            @Primary
            MutableClock mutableClock() {
                return new MutableClock(Instant.parse("2026-11-22T23:59:59Z"));
            }
        }

        @Autowired
        MutableClock clock;

        @Autowired
        PromotionService promotions;

        @Test
        @DisplayName("Wzorzec 5: zmienny zegar — sekundę przed, w trakcie i po promocji, w jednym kontekście")
        void movingClock() {
            assertThat(promotions.isBlackWeek()).isFalse();

            clock.set(Instant.parse("2026-11-23T00:00:00Z"));
            assertThat(promotions.isBlackWeek()).isTrue();

            clock.set(Instant.parse("2026-11-30T00:00:00Z"));
            assertThat(promotions.isBlackWeek()).isFalse();
        }
    }
}
