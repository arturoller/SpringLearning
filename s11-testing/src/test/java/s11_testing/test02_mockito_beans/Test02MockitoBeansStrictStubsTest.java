package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Sekcja 1 lekcji Test02MockitoBeans: tryb ścisły (STRICT_STUBS), którego domyślnie używa MockitoExtension.
 * Nieużyte „nauczenie” atrapy (stubbing) to błąd testu. Pokazujemy to na sesji Mockito ({@code MockitoSession}) —
 * tym samym mechanizmie, który MockitoExtension uruchamia wokół każdego testu.
 */
class Test02MockitoBeansStrictStubsTest {

    static class Mocks {
        @Mock
        ProductCatalog catalog;
    }

    @Test
    @DisplayName("1. STRICT_STUBS: nauczone, ale nieużyte wywołanie atrapy → UnnecessaryStubbingException")
    void unnecessaryStubbing() {
        var mocks = new Mocks();
        MockitoSession session = Mockito.mockitoSession()
                .initMocks(mocks)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();

        when(mocks.catalog.getBySku("SPO-001")).thenReturn(new Product("SPO-001", "Kawa ziarnista 1kg", 6499));
        // ...a test nigdy nie woła getBySku("SPO-001") — np. po zmianie kodu ta linia stała się martwa.

        assertThatThrownBy(session::finishMocking)   // to samo MockitoExtension robi po każdym teście
                .isInstanceOf(UnnecessaryStubbingException.class)
                .hasMessageContaining("Unnecessary stubbings detected.");
    }
}
