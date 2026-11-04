package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Testy-ĆWICZENIA lekcji Test05TestConfiguration: TY piszesz test (usuń linię z UnsupportedOperationException;
 * adnotacje Springa dopisz na klasie — najlepiej każde ćwiczenie w osobnej klasie albo w {@code @Nested}, bo każde
 * potrzebuje innej konfiguracji). @Tag("cwiczenie") — uruchamiasz sam.
 * Rozwiązania wzorcowe: pakiet s11_testing.solutions.test05_test_configuration.
 */
@Tag("cwiczenie")
class Test05TestConfigurationExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: zagnieżdżona @TestConfiguration z zegarem @Primary na 2026-11-30T00:00Z — promocja już się skończyła")
    void exercise1() {
        // TODO: @SpringBootTest(webEnvironment = NONE) + @ActiveProfiles("test05"); static class z @TestConfiguration,
        //       @Bean @Primary Clock; assertThat(promotions.isBlackWeek()).isFalse()
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: @SpringBootTest(properties = \"test05.shop.name=Mój sklep\") — ShopInfo zwraca nazwę z testu, nie z profilu")
    void exercise2() {
        // TODO: properties = "test05.shop.name=Mój sklep"; @Autowired ShopInfo; assertThat(shop.name()).isEqualTo(...)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: @DynamicPropertySource z PriceStubServer — cena MYSZ z atrapy to 59.00")
    void exercise3() {
        // TODO: static final PriceStubServer STUB = PriceStubServer.start(Map.of("MYSZ", "59.00"));
        //       @DynamicPropertySource static void ...(DynamicPropertyRegistry r) { r.add("test05.price-service.url", STUB::baseUrl); }
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: ApplicationContextRunner — przy test05.newsletter.enabled=false send() zwraca 'Newsletter wyłączony'")
    void exercise4() {
        // TODO: new ApplicationContextRunner().withUserConfiguration(NewsletterConfig.class).withPropertyValues(...).run(context -> ...)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 5: własny ZMIENNY zegar testowy (podklasa Clock) jako bean @Primary — w jednym teście przed, w trakcie i po promocji")
    void exercise5() {
        // TODO: class MutableClock extends Clock { volatile Instant now; void set(Instant i); instant(), getZone(), withZone() }
        //       @TestConfiguration z @Bean @Primary MutableClock; w teście clock.set(...) i sprawdzanie isBlackWeek()
        throw new UnsupportedOperationException("TODO");
    }
}
