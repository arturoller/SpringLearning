package s11_testing.solutions.test05_test_configuration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import s11_testing.test05_test_configuration.EuroPriceCalculator;
import s11_testing.test05_test_configuration.ExchangeRateClient;
import s11_testing.test05_test_configuration.PromotionService;
import s11_testing.test05_test_configuration.Test05Application;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test05TestConfiguration (te same testy co w
 * Test05TestConfigurationExercisesTest, już uzupełnione). {@code @Tag("wzorzec")} — biegną zawsze.
 * Konfigurację aplikacji wskazujemy jawnie ({@code classes = Test05Application.class}), bo pakiet {@code solutions…}
 * nie leży pod pakietem lekcji.
 */
@Tag("wzorzec")
class Test05TestConfigurationSolutionsTest {

    @Nested
    @SpringBootTest(classes = Test05Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @Import(Solution1.LastEveningClock.class)   // przy classes = ... zagnieżdżona konfiguracja NIE jest wykrywana sama
    class Solution1 {

        @TestConfiguration
        static class LastEveningClock {
            @Bean
            @Primary
            Clock lastEveningClock() {
                ZoneId warsaw = ZoneId.of("Europe/Warsaw");
                // 23:30 w Warszawie (zima: UTC+1) = 22:30 UTC; ZonedDateTime liczy przesunięcie za nas.
                return Clock.fixed(ZonedDateTime.of(2026, 11, 30, 23, 30, 0, 0, warsaw).toInstant(), warsaw);
            }
        }

        @Autowired
        Clock clock;

        @Autowired
        PromotionService promotions;

        @Test
        @DisplayName("Wzorzec 1: ostatni wieczór promocji (30.11.2026, 23:30 w Warszawie)")
        void solution1() {
            assertThat(LocalDateTime.now(clock)).isEqualTo(LocalDateTime.of(2026, 11, 30, 23, 30));
            assertThat(promotions.isBlackWeekend()).isTrue();
        }
    }

    @Nested
    @SpringBootTest(classes = Test05Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    @TestPropertySource(properties = "shop.name=Sklep ćwiczeniowy")
    class Solution2 {

        @Autowired
        Environment environment;

        @Test
        @DisplayName("Wzorzec 2: właściwość z testu wygrywa z plikiem profilu")
        void solution2() {
            assertThat(environment.getProperty("shop.name")).isEqualTo("Sklep ćwiczeniowy");
            assertThat(environment.getProperty("shop.promotion-banner")).isEqualTo("Czarny weekend: -20%");
        }
    }

    @Nested
    @SpringBootTest(classes = Test05Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Solution3 {

        static final HttpServer STUB = startStub("4.31");

        @DynamicPropertySource
        static void exchangeRatesUrl(DynamicPropertyRegistry registry) {
            registry.add("shop.exchange.base-url", () -> "http://localhost:" + STUB.getAddress().getPort());
        }

        @AfterAll
        static void stop() {
            STUB.stop(0);
        }

        @Autowired
        EuroPriceCalculator calculator;

        @Test
        @DisplayName("Wzorzec 3: kalkulator euro korzysta z zaślepki pod losowym portem (kurs 4.31)")
        void solution3() {
            assertThat(calculator.toEuro(new BigDecimal("129.30"))).isEqualByComparingTo("30.00");
        }
    }

    @Nested
    @SpringBootTest(classes = Test05Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Solution4 {

        @MockitoBean   // atrapa zamiast klienta HTTP (lekcja s11_testing/Test02MockitoBeans); można też @TestBean
        ExchangeRateClient rates;

        @Autowired
        EuroPriceCalculator calculator;

        @Test
        @DisplayName("Wzorzec 4: kurs 4.10 bez serwera — 64.99 zł to 15.85 euro")
        void solution4() {
            given(rates.eurRate()).willReturn(new BigDecimal("4.10"));

            assertThat(calculator.toEuro(new BigDecimal("64.99"))).isEqualByComparingTo("15.85");
        }
    }

    static HttpServer startStub(String rate) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/rates/EUR", exchange -> {
                byte[] body = rate.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
