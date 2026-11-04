package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 2 (PUŁAPKA): {@code @WebMvcTest} NIE tworzy serwisów, więc kontroler wymagający {@code ProductService} nie
 * ma co wstrzyknąć i cały kontekst się nie podnosi. Błąd ładowania pokazujemy ręcznie przez {@code TestContextManager}
 * (to ten sam mechanizm, którego używa JUnit — tu wywołujemy go sami, żeby złapać wyjątek). Drugi przypadek:
 * {@code @SpringBootTest} bez {@code @AutoConfigureMockMvc} nie ma beana MockMvc.
 */
class Test01MissingServiceTest {

    @Test
    @DisplayName("Sekcja 2: @WebMvcTest bez atrapy serwisu → kontekst się nie ładuje (NoSuchBeanDefinitionException)")
    void contextFailsWithoutServiceMock() {
        TestContextManager manager = new TestContextManager(WebMvcWithoutServiceCase.class);

        assertThatThrownBy(() -> manager.getTestContext().getApplicationContext())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load ApplicationContext")
                .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                .rootCause().hasMessageContaining("ProductService");
    }

    @Test
    @DisplayName("Sekcja 3: @SpringBootTest bez @AutoConfigureMockMvc → wstrzyknięcie MockMvc się nie udaje")
    void mockMvcNeedsAutoConfigureMockMvc() {
        TestContextManager manager = new TestContextManager(SpringBootTestWithoutMockMvcCase.class);

        assertThatThrownBy(() -> manager.prepareTestInstance(new SpringBootTestWithoutMockMvcCase()))
                .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                .rootCause().hasMessageContaining("MockMvc");
    }
}
