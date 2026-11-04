package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test01TestSlices: pamięć podręczna kontekstów (context cache). Spring Test trzyma uruchomione
 * konteksty w pamięci, w jednym statycznym cache na cały proces testów, i wydaje ten sam, gdy KLUCZ jest taki sam.
 * Klucz to cała konfiguracja: klasy, profile, właściwości, zestaw {@code @MockitoBean}, rodzaj środowiska webowego…
 * Test ładuje konteksty ręcznie przez {@code TestContextManager} — wynik nie zależy od kolejności klas testowych.
 */
class Test01ContextCacheTest {

    private static ApplicationContext contextOf(Class<?> testClass) {
        return new TestContextManager(testClass).getTestContext().getApplicationContext();
    }

    @Test
    @DisplayName("Sekcja 4: dwie klasy z TĄ SAMĄ konfiguracją dostają TEN SAM obiekt ApplicationContext")
    void sameConfigurationSameContext() {
        ApplicationContext a = contextOf(ContextCacheCases.SameConfigA.class);
        ApplicationContext b = contextOf(ContextCacheCases.SameConfigB.class);

        assertThat(b).isSameAs(a);
    }

    @Test
    @DisplayName("Sekcja 4: inna właściwość w @SpringBootTest(properties) = nowy, drugi kontekst (i drugi, wolny start)")
    void otherPropertyOtherContext() {
        ApplicationContext a = contextOf(ContextCacheCases.SameConfigA.class);
        ApplicationContext other = contextOf(ContextCacheCases.OtherProperty.class);

        assertThat(other).isNotSameAs(a);
        assertThat(other.getEnvironment().getProperty("demo.flag")).isEqualTo("true");
    }

    @Test
    @DisplayName("Sekcja 4: ten sam zestaw @MockitoBean = wspólny kontekst; inny zestaw = osobny")
    void mockitoBeanSetIsPartOfTheKey() {
        ApplicationContext plain = contextOf(ContextCacheCases.SameConfigA.class);
        ApplicationContext mockA = contextOf(ContextCacheCases.WithMockA.class);
        ApplicationContext mockB = contextOf(ContextCacheCases.WithMockB.class);
        ApplicationContext otherMock = contextOf(ContextCacheCases.WithOtherMock.class);

        assertThat(mockB).as("ta sama atrapa ProductService w dwóch klasach").isSameAs(mockA);
        assertThat(mockA).as("atrapa zmienia konfigurację").isNotSameAs(plain);
        assertThat(otherMock).as("atrapa innego beana").isNotSameAs(mockA).isNotSameAs(plain);
    }

    @Test
    @DisplayName("Sekcja 4: unieważnienie kontekstu (to robi @DirtiesContext) zamyka go, a następny test dostaje nowy")
    void dirtiedContextIsReplaced() {
        TestContextManager manager = new TestContextManager(ContextCacheCases.ForDirtying.class);
        ConfigurableApplicationContext before = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        assertThat(before.isActive()).isTrue();

        manager.getTestContext().markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);   // to samo, co @DirtiesContext

        ApplicationContext after = contextOf(ContextCacheCases.ForDirtying.class);
        assertThat(before.isActive()).as("stary kontekst zamknięty").isFalse();
        assertThat(after).isNotSameAs(before);
    }
}
