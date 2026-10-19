package s11_testing.test05_test_configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Lekcja Test05TestConfiguration, sekcja 1: OSOBNA (nie zagnieżdżona) klasa {@code @TestConfiguration}.
 * Skanowanie komponentów jej NIE znajduje — test, który jej chce, musi napisać {@code @Import(Test05FixedClockConfiguration.class)}.
 * Zegar zatrzymany 1 grudnia 2026 o 12:00 czasu polskiego — DZIEŃ PO czarnym weekendzie.
 */
@TestConfiguration   // test configuration = konfiguracja tylko dla testów
public class Test05FixedClockConfiguration {

    public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Bean
    @Primary   // primary = główny: gdy są dwa beany Clock, wstrzykiwany jest ten
    Clock clockAfterPromotion() {
        return Clock.fixed(Instant.parse("2026-12-01T11:00:00Z"), WARSAW);
    }
}
