package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 6 lekcji Test01TestSlices: cache (pamięć podręczna) kontekstów. Spring trzyma gotowe konteksty w statycznym
 * cache i oddaje TEN SAM obiekt każdej klasie testowej o identycznej konfiguracji.
 *
 * <p>Jak to sprawdzamy bez zależności od kolejności testów? Klasy-„wzorce” poniżej nie mają metod testowych — to tylko
 * nośniki adnotacji. {@code TestContextManager} (zarządca kontekstu testu) robi dla nich DOKŁADNIE to, co robi
 * SpringExtension przed zwykłym testem: buduje klucz konfiguracji i pobiera kontekst z cache (albo go tworzy).</p>
 */
class Test01ContextCachingTest {

    @SpringBootTest
    static class ConfigA {
    }

    @SpringBootTest
    static class ConfigB {   // inna klasa, ale konfiguracja identyczna jak ConfigA
    }

    @SpringBootTest(properties = "shop.name=Sklep testowy")
    static class WithExtraProperty {
    }

    @SpringBootTest
    static class WithMockitoBean {
        @MockitoBean
        ProductService service;   // atrapa = inny zestaw beanów = inny klucz cache
    }

    static ApplicationContext contextFor(Class<?> testClass) {
        return new TestContextManager(testClass).getTestContext().getApplicationContext();
    }

    @Test
    @DisplayName("6. Ta sama konfiguracja w dwóch klasach → TEN SAM obiekt kontekstu (cache)")
    void sameConfigurationSameContext() {
        assertThat(contextFor(ConfigA.class)).isSameAs(contextFor(ConfigB.class));
    }

    @Test
    @DisplayName("6. Dodatkowa właściwość w @SpringBootTest(properties) → NOWY kontekst")
    void differentPropertiesNewContext() {
        ApplicationContext withProperty = contextFor(WithExtraProperty.class);
        assertThat(withProperty).isNotSameAs(contextFor(ConfigA.class));
        assertThat(withProperty.getEnvironment().getProperty("shop.name")).isEqualTo("Sklep testowy");
    }

    @Test
    @DisplayName("6. @MockitoBean w klasie testowej → NOWY kontekst (inny zestaw beanów)")
    void mockitoBeanNewContext() {
        assertThat(contextFor(WithMockitoBean.class)).isNotSameAs(contextFor(ConfigA.class));
    }
}
