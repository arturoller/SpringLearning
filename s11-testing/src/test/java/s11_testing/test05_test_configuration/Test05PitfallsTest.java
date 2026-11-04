package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 1 lekcji Test05TestConfiguration — PUŁAPKI podmiany beana przez {@code @TestConfiguration}. Konteksty
 * „klas-przypadków” z Test05PitfallCases ładujemy ręcznie przez {@code TestContextManager} (menedżer kontekstu
 * testów — to samo robi JUnit przed każdą klasą testową), żeby błąd startu był WYNIKIEM testu, a nie czerwonym testem.
 */
class Test05PitfallsTest {

    private static ApplicationContext load(Class<?> testCase) throws Exception {
        return new TestContextManager(testCase).getTestContext().getApplicationContext();
    }

    @Test
    @DisplayName("Sekcja 1, PUŁAPKA: zegar testowy o tej samej nazwie 'clock' → kontekst nie wstaje (BeanDefinitionOverrideException)")
    void sameBeanNameFails() {
        assertThatThrownBy(() -> load(Test05PitfallCases.SameBeanNameCase.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load ApplicationContext")
                .rootCause()
                .isInstanceOf(BeanDefinitionOverrideException.class)
                .hasMessageContaining("clock");
    }

    @Test
    @DisplayName("Sekcja 1, PUŁAPKA: drugi zegar bez @Primary → serwis dostaje zegar SYSTEMOWY (dopasowanie po nazwie parametru 'clock')")
    void secondClockWithoutPrimaryIsIgnored() throws Exception {
        ApplicationContext context = load(Test05PitfallCases.SecondClockWithoutPrimaryCase.class);

        assertThat(context.getBeansOfType(Clock.class)).containsOnlyKeys("clock", "testClock");
        PromotionService promotions = context.getBean(PromotionService.class);
        assertThat(promotions.clock()).isSameAs(context.getBean("clock"))
                .isNotSameAs(context.getBean("testClock"));
    }

    @Test
    @DisplayName("Sekcja 1: osobna @TestConfiguration bez @Import NIE jest wykrywana skanowaniem — zostaje tylko zegar aplikacji")
    void separateTestConfigurationNeedsImport() throws Exception {
        ApplicationContext context = load(Test05PitfallCases.NotImportedCase.class);

        assertThat(context.getBeansOfType(Clock.class)).containsOnlyKeys("clock");
    }
}
