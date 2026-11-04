package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 5 lekcji Test02MockitoBeans: pułapki atrap — {@code @MockBean} ze starych tutoriali, atrapa beana, którego
 * nie ma, szpieg na nieistniejącym beanie.
 */
class Test02PitfallsTest {

    @Test
    @DisplayName("Sekcja 1: adnotacje @MockBean i @SpyBean z tutoriali dla Boot 2/3 NIE ISTNIEJĄ w Boot 4 — import się nie skompiluje")
    void oldAnnotationsAreGone() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.test.mock.mockito.MockBean"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.test.mock.mockito.SpyBean"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThat(org.springframework.test.context.bean.override.mockito.MockitoBean.class).isAnnotation();   // nowe są w Spring Test
    }

    @Test
    @DisplayName("Sekcja 5: @MockitoBean na beanie, którego NIE MA, po cichu TWORZY nowy bean-atrapę (a nie zgłasza błędu)")
    void mockOfMissingBeanCreatesNewBean() {
        TestContextManager manager = new TestContextManager(MockitoBeanPitfallCases.MockOfMissingBean.class);

        ApplicationContext context = manager.getTestContext().getApplicationContext();

        assertThat(context.getBeanNamesForType(MockitoBeanPitfallCases.MissingPort.class)).hasSize(1);
        assertThat(Mockito.mockingDetails(context.getBean(MockitoBeanPitfallCases.MissingPort.class)).isMock()).isTrue();
    }

    @Test
    @DisplayName("Sekcja 5: enforceOverride = true — to samo wywala kontekst, bo nie ma czego zastępować (literówka wyjdzie od razu)")
    void enforceOverrideFailsForMissingBean() {
        TestContextManager manager = new TestContextManager(MockitoBeanPitfallCases.StrictMockOfMissingBean.class);

        assertThatThrownBy(() -> manager.getTestContext().getApplicationContext())
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .rootCause().hasMessageContaining("MissingPort");
    }

    @Test
    @DisplayName("Sekcja 5: @MockitoSpyBean na beanie, którego NIE MA → kontekst się nie ładuje (szpieg wymaga prawdziwego beana)")
    void spyOfMissingBeanFails() {
        TestContextManager manager = new TestContextManager(MockitoBeanPitfallCases.SpyOfMissingBean.class);

        assertThatThrownBy(() -> manager.getTestContext().getApplicationContext())
                .isInstanceOf(IllegalStateException.class)
                .rootCause().hasMessageContaining("MissingPort");
    }
}
