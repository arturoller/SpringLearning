package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 3, 5 i 6 lekcji Test05TestConfiguration: skąd test bierze właściwości i która wygrywa, gdy kilka źródeł
 * ustawia TĘ SAMĄ właściwość {@code shop.name}. Każda klasa-nośnik dokłada jedno źródło więcej.
 */
class Test05TestConfigurationPropertiesTest {

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test05")   // plik application-test05.properties: shop.name=Sklep z profilu test05
    static class ProfileOnly {
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "shop.name=z @SpringBootTest(properties)")
    @ActiveProfiles("test05")
    static class PlusSpringBootTestProperties {
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "shop.name=z @SpringBootTest(properties)")
    @TestPropertySource(properties = "shop.name=z @TestPropertySource")   // test property source = źródło właściwości testu
    @ActiveProfiles("test05")
    static class PlusTestPropertySource {
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "shop.name=z @SpringBootTest(properties)")
    @TestPropertySource(properties = "shop.name=z @TestPropertySource")
    @ActiveProfiles("test05")
    static class PlusDynamicPropertySource {
        @DynamicPropertySource
        static void shopName(DynamicPropertyRegistry registry) {
            registry.add("shop.name", () -> "z @DynamicPropertySource");
        }
    }

    static String shopNameIn(Class<?> testClass) {
        return contextFor(testClass).getEnvironment().getProperty("shop.name");
    }

    static ApplicationContext contextFor(Class<?> testClass) {
        return new TestContextManager(testClass).getTestContext().getApplicationContext();
    }

    @Test
    @DisplayName("5. @ActiveProfiles(\"test05\") wczytuje application-test05.properties")
    void activeProfileLoadsProfileFile() {
        ApplicationContext context = contextFor(ProfileOnly.class);

        assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("test05");
        assertThat(context.getEnvironment().getProperty("shop.name")).isEqualTo("Sklep z profilu test05");
        assertThat(context.getEnvironment().getProperty("shop.promotion-banner")).isEqualTo("Czarny weekend: -20%");
    }

    @Test
    @DisplayName("3. Kolejność ważności: @DynamicPropertySource > @TestPropertySource > @SpringBootTest(properties) > profil")
    void precedence() {
        assertThat(shopNameIn(ProfileOnly.class)).isEqualTo("Sklep z profilu test05");
        assertThat(shopNameIn(PlusSpringBootTestProperties.class)).isEqualTo("z @SpringBootTest(properties)");
        assertThat(shopNameIn(PlusTestPropertySource.class)).isEqualTo("z @TestPropertySource");
        assertThat(shopNameIn(PlusDynamicPropertySource.class)).isEqualTo("z @DynamicPropertySource");
    }

    @Test
    @DisplayName("6. Każde inne źródło właściwości = inny klucz = osobny kontekst w pamięci podręcznej")
    void differentPropertiesDifferentContexts() {
        assertThat(contextFor(ProfileOnly.class)).isNotSameAs(contextFor(PlusSpringBootTestProperties.class));
        assertThat(contextFor(ProfileOnly.class)).isSameAs(contextFor(ProfileOnly.class));
    }
}
