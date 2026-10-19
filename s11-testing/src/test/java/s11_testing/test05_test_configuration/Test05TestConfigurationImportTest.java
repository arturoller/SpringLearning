package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestContextManager;

import java.math.BigDecimal;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1–2 lekcji Test05TestConfiguration: osobna klasa {@code @TestConfiguration} (Test05FixedClockConfiguration)
 * działa tylko z {@code @Import}; zwykła {@code @Configuration} z src/test wpada do kontekstu sama (PUŁAPKA);
 * bean testowy o tej samej nazwie co bean aplikacji → błąd startu. Konteksty budujemy przez TestContextManager
 * (jak w s11_testing/Test01TestSlices, sekcja 6) — klasy-nośniki adnotacji nie mają metod testowych.
 */
class Test05TestConfigurationImportTest {

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class WithoutImport {
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @Import(Test05FixedClockConfiguration.class)   // import = dołącz tę konfigurację do kontekstu testu
    static class WithImport {
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class SameBeanName {
        @TestConfiguration
        static class ClockWithSameName {
            @Bean
            Clock clock() {   // ta sama nazwa co @Bean clock() w Test05Application
                return Clock.systemUTC();
            }
        }
    }

    @SpringBootTest(classes = Test05Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class ExplicitClasses {
        @TestConfiguration
        static class NestedClock {
            @Bean
            Clock nestedClock() {
                return Clock.systemUTC();
            }
        }
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class NestedPlainConfiguration {
        @Configuration   // ZWYKŁA @Configuration zagnieżdżona w klasie testowej
        static class OnlyThis {
            @Bean
            Clock onlyClock() {
                return Clock.systemUTC();
            }
        }
    }

    static ApplicationContext contextFor(Class<?> testClass) {
        return new TestContextManager(testClass).getTestContext().getApplicationContext();
    }

    @Test
    @DisplayName("1. Osobna @TestConfiguration bez @Import NIE jest skanowana — zegara z niej nie ma")
    void separateTestConfigurationIsNotScanned() {
        assertThat(contextFor(WithoutImport.class).containsBean("clockAfterPromotion")).isFalse();
    }

    @Test
    @DisplayName("1. Z @Import: zegar 1.12.2026 (po promocji) → cena bez rabatu")
    void importedTestConfigurationIsUsed() {
        ApplicationContext context = contextFor(WithImport.class);
        PromotionService promotions = context.getBean(PromotionService.class);

        assertThat(promotions.isBlackWeekend()).isFalse();
        assertThat(promotions.priceFor(new BigDecimal("64.99"))).isEqualByComparingTo("64.99");
    }

    @Test
    @DisplayName("1. PUŁAPKA: zwykła @Configuration z src/test JEST skanowana — jej bean jest w każdym kontekście lekcji")
    void plainConfigurationInTestSourcesIsScanned() {
        assertThat(contextFor(WithoutImport.class).containsBean("accidentallyScanned")).isTrue();
    }

    @Test
    @DisplayName("1. Zagnieżdżona ZWYKŁA @Configuration działa ZAMIAST aplikacji — serwisów lekcji w kontekście nie ma")
    void nestedPlainConfigurationReplacesApplication() {
        ApplicationContext context = contextFor(NestedPlainConfiguration.class);

        assertThat(context.containsBean("onlyClock")).isTrue();
        assertThat(context.getBeanNamesForType(PromotionService.class)).isEmpty();
    }

    @Test
    @DisplayName("1. PUŁAPKA: przy @SpringBootTest(classes = ...) zagnieżdżona @TestConfiguration jest POMIJANA")
    void explicitClassesSkipNestedTestConfiguration() {
        assertThat(contextFor(ExplicitClasses.class).containsBean("nestedClock")).isFalse();
    }

    @Test
    @DisplayName("2. Co się stanie: @Bean clock() w teście, a aplikacja ma już bean „clock” → BeanDefinitionOverrideException")
    void sameBeanNameFailsContext() {
        assertThatThrownBy(() -> contextFor(SameBeanName.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to load ApplicationContext")
                .rootCause()
                .isInstanceOf(BeanDefinitionOverrideException.class)
                .hasMessageStartingWith("Invalid bean definition with name 'clock' defined in "
                        + "s11_testing.test05_test_configuration.Test05TestConfigurationImportTest$SameBeanName$ClockWithSameName: "
                        + "Cannot register bean definition")
                .hasMessageEndingWith("bound.");
    }
}
