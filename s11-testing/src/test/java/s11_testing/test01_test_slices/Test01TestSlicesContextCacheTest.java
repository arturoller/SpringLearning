package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 6 lekcji Test01TestSlices: pamięć podręczna kontekstów (context cache). Spring trzyma gotowe konteksty
 * w statycznej pamięci (jednej na całą JVM z testami) i oddaje TEN SAM obiekt każdej klasie testowej o identycznej
 * konfiguracji. Liczymy STARTY kontekstu licznikiem {@link StartCounter}.
 *
 * <p>Jak to sprawdzamy bez zależności od kolejności testów? Klasy-„wzorce” poniżej nie mają metod testowych — to tylko
 * nośniki adnotacji. {@code TestContextManager} (zarządca kontekstu testu) robi dla nich DOKŁADNIE to, co
 * SpringExtension przed zwykłym testem: buduje klucz konfiguracji i bierze kontekst z pamięci (albo go tworzy).
 * Właściwość {@code test01.cache} sprawia, że te konfiguracje są unikalne — żaden inny test ich nie użyje.</p>
 */
class Test01TestSlicesContextCacheTest {

    /** Licznik startów: bean, którego konstruktor wykonuje się raz na każdy NOWO utworzony kontekst. */
    static class StartCounter {
        static final AtomicInteger STARTS = new AtomicInteger();

        StartCounter() {
            STARTS.incrementAndGet();
        }
    }

    @SpringBootTest(properties = "test01.cache=demo")
    @Import(StartCounter.class)
    static class ConfigA {
    }

    @SpringBootTest(properties = "test01.cache=demo")
    @Import(StartCounter.class)
    static class ConfigB {   // inna klasa, ale konfiguracja identyczna jak ConfigA
    }

    @SpringBootTest(properties = {"test01.cache=demo", "shop.name=Sklep testowy"})
    @Import(StartCounter.class)
    static class WithExtraProperty {   // jedna właściwość więcej
    }

    @SpringBootTest(properties = "test01.cache=demo")
    @Import(StartCounter.class)
    static class WithMockitoBean {
        @MockitoBean
        ProductService service;   // atrapa = inny zestaw beanów = inny klucz w pamięci podręcznej
    }

    @SpringBootTest(properties = "test01.cache=dirty")
    @Import(StartCounter.class)
    @DirtiesContext   // dirties context = „brudzi kontekst”: po klasie testowej kontekst jest zamykany i usuwany z pamięci
    static class Dirty {
    }

    static ApplicationContext contextFor(Class<?> testClass) {
        return new TestContextManager(testClass).getTestContext().getApplicationContext();
    }

    @Test
    @DisplayName("6. Ta sama konfiguracja w dwóch klasach → TEN SAM kontekst, zero nowych startów")
    void sameConfigurationSameContext() {
        ApplicationContext a = contextFor(ConfigA.class);   // może wystartować teraz albo już być w pamięci
        int startsBefore = StartCounter.STARTS.get();

        ApplicationContext b = contextFor(ConfigB.class);

        assertThat(StartCounter.STARTS.get()).as("drugi kontekst nie startował").isEqualTo(startsBefore);
        assertThat(b).isSameAs(a);
    }

    @Test
    @DisplayName("6. Dodatkowa właściwość w @SpringBootTest(properties) → NOWY kontekst (+1 start)")
    void differentPropertiesNewContext() {
        ApplicationContext a = contextFor(ConfigA.class);
        int startsBefore = StartCounter.STARTS.get();

        ApplicationContext withProperty = contextFor(WithExtraProperty.class);

        assertThat(StartCounter.STARTS.get()).isEqualTo(startsBefore + 1);
        assertThat(withProperty).isNotSameAs(a);
        assertThat(withProperty.getEnvironment().getProperty("shop.name")).isEqualTo("Sklep testowy");
    }

    @Test
    @DisplayName("6. @MockitoBean w klasie testowej → NOWY kontekst (+1 start), bo inny zestaw beanów")
    void mockitoBeanNewContext() {
        ApplicationContext a = contextFor(ConfigA.class);
        int startsBefore = StartCounter.STARTS.get();

        ApplicationContext withMock = contextFor(WithMockitoBean.class);

        assertThat(StartCounter.STARTS.get()).isEqualTo(startsBefore + 1);
        assertThat(withMock).isNotSameAs(a);
    }

    @Test
    @DisplayName("6. @DirtiesContext → po klasie kontekst jest zamykany, następna klasa startuje go od nowa (+1)")
    void dirtiesContextClosesAndRestarts() throws Exception {
        TestContextManager manager = new TestContextManager(Dirty.class);
        var first = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();

        // Po ostatnim teście klasy JUnit woła afterTestClass(); @DirtiesContext każe wtedy zamknąć kontekst.
        manager.afterTestClass();
        assertThat(first.isActive()).as("kontekst zamknięty").isFalse();

        int startsBefore = StartCounter.STARTS.get();
        ApplicationContext second = contextFor(Dirty.class);

        assertThat(StartCounter.STARTS.get()).isEqualTo(startsBefore + 1);
        assertThat(second).isNotSameAs(first);
    }
}
