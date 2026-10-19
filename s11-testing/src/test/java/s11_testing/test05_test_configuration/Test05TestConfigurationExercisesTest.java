package s11_testing.test05_test_configuration;

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
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Test05TestConfiguration. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Uzupełnij miejsca z TODO i usuń {@code todo()}; asercje są gotowe. Uwaga: gdy {@code todo()} stoi w metodzie
 * {@code @Bean} albo {@code @DynamicPropertySource}, test pada już przy starcie kontekstu — „Failed to load
 * ApplicationContext”, a na dnie (Caused by) jest {@code UnsupportedOperationException: TODO}.
 */
@Tag("cwiczenie")
class Test05TestConfigurationExercisesTest {

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie zastąpisz swoim kodem. */
    static <T> T todo() {
        throw new UnsupportedOperationException("TODO");
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Exercise1 {

        @TestConfiguration
        static class LastEveningClock {
            @Bean
            @Primary
            Clock lastEveningClock() {
                // TODO: zegar zatrzymany 30.11.2026 o 23:30 CZASU POLSKIEGO (strefa Europe/Warsaw)
                return todo();
            }
        }

        @Autowired
        Clock clock;

        @Autowired
        PromotionService promotions;

        @Test
        @DisplayName("Ćwiczenie 1: ostatni wieczór promocji (30.11.2026, 23:30 w Warszawie)")
        void exercise1() {
            assertThat(LocalDateTime.now(clock)).isEqualTo(LocalDateTime.of(2026, 11, 30, 23, 30));
            assertThat(promotions.isBlackWeekend()).isTrue();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    // TODO (ćwiczenie 2): dodaj adnotację, która NA CZAS TEGO TESTU ustawia shop.name=Sklep ćwiczeniowy
    class Exercise2 {

        @Autowired
        Environment environment;

        @Test
        @DisplayName("Ćwiczenie 2: właściwość z testu wygrywa z plikiem profilu")
        void exercise2() {
            todo();   // TODO: usuń tę linię po dodaniu adnotacji

            assertThat(environment.getProperty("shop.name")).isEqualTo("Sklep ćwiczeniowy");
            assertThat(environment.getProperty("shop.promotion-banner")).isEqualTo("Czarny weekend: -20%");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Exercise3 {

        static final HttpServer STUB = startStub("4.31");

        @DynamicPropertySource
        static void exchangeRatesUrl(DynamicPropertyRegistry registry) {
            // TODO: zarejestruj shop.exchange.base-url = adres zaślepki STUB (localhost + jej port)
            todo();
        }

        @AfterAll
        static void stop() {
            STUB.stop(0);
        }

        @Autowired
        EuroPriceCalculator calculator;

        @Test
        @DisplayName("Ćwiczenie 3: kalkulator euro korzysta z zaślepki pod losowym portem (kurs 4.31)")
        void exercise3() {
            assertThat(calculator.toEuro(new BigDecimal("129.30"))).isEqualByComparingTo("30.00");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Exercise4 {

        // TODO (ćwiczenie 4): podmień bean ExchangeRateClient tak, żeby eurRate() zwracało 4.10 — BEZ żadnego serwera

        @Autowired
        EuroPriceCalculator calculator;

        @Test
        @DisplayName("Ćwiczenie 4: kurs 4.10 bez serwera — 64.99 zł to 15.85 euro")
        void exercise4() {
            todo();   // TODO: usuń tę linię po podmianie beana

            assertThat(calculator.toEuro(new BigDecimal("64.99"))).isEqualByComparingTo("15.85");
        }
    }

    /** Lokalna zaślepka kursów: GET /rates/EUR → podany tekst. Port 0 = wolny port wybrany przez system. */
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
