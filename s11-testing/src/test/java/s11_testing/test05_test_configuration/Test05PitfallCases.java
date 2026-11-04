package s11_testing.test05_test_configuration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Pomocnicze „klasy testowe” BEZ metod testowych — JUnit ich nie uruchamia; Test05PitfallsTest ładuje ich konteksty
 * ręcznie ({@code TestContextManager}), żeby pokazać pułapki z sekcji 1 lekcji Test05TestConfiguration.
 */
final class Test05PitfallCases {

    static final Instant BLACK_FRIDAY = Instant.parse("2026-11-27T10:00:00Z");

    private Test05PitfallCases() {
    }

    /** Zegar testowy o TEJ SAMEJ nazwie co w aplikacji ("clock") — nadpisywanie beanów jest w Boot domyślnie wyłączone. */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    static class SameBeanNameCase {
        @TestConfiguration(proxyBeanMethods = false)
        static class Config {
            @Bean
            Clock clock() {
                return Clock.fixed(BLACK_FRIDAY, ZoneOffset.UTC);
            }
        }
    }

    /** Drugi zegar o innej nazwie, BEZ @Primary — kontekst wstaje, ale który zegar dostanie PromotionService? */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    static class SecondClockWithoutPrimaryCase {
        @TestConfiguration(proxyBeanMethods = false)
        static class Config {
            @Bean
            Clock testClock() {
                return Clock.fixed(BLACK_FRIDAY, ZoneOffset.UTC);
            }
        }
    }

    /** Osobna FixedClockTestConfig istnieje w pakiecie testów, ale ta klasa jej NIE importuje. */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")
    static class NotImportedCase {
    }
}
