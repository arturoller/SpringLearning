package s11_testing.test03_mock_mvc_tester;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Sekcja 5 lekcji Test03MockMvcTester: nieobsłużony wyjątek kontrolera. Klasyczny MockMvc rzuca go z
 * {@code perform} jako {@code ServletException}, a MockMvcTester NIE rzuca — zapisuje porażkę w wyniku, którą
 * sprawdzasz przez {@code hasFailed()} i {@code failure()}.
 */
@WebMvcTest(ProductController.class)
@Import(ProductService.class)
class Test03UnhandledExceptionTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 5: klasyczny MockMvc — perform rzuca ServletException, a przyczyną jest nasz wyjątek")
    void classicThrowsServletException() {
        assertThatThrownBy(() -> mockMvc.perform(get("/test03/boom")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Awaria serwisu");
    }

    @Test
    @DisplayName("Sekcja 5: MockMvcTester — hasFailed() i failure() zamiast łapania wyjątku (to ten sam ServletException)")
    void testerRecordsFailure() {
        assertThat(mvc.get().uri("/test03/boom"))
                .hasFailed()
                .failure().isInstanceOf(ServletException.class)                  // tak samo opakowany jak w klasycznym MockMvc
                .hasRootCauseInstanceOf(IllegalStateException.class).hasRootCauseMessage("Awaria serwisu");
    }

    @Test
    @DisplayName("Sekcja 5: żądanie bez porażki przechodzi doesNotHaveFailed(); sprawdzanie statusu przy porażce kończy się AssertionError")
    void statusCheckOnFailedRequest() {
        assertThat(mvc.get().uri("/test03/products/1")).doesNotHaveFailed();

        assertThatThrownBy(() -> assertThat(mvc.get().uri("/test03/boom")).hasStatus(500))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Awaria serwisu");   // komunikat wskazuje na wyjątek, nie na sam kod statusu
    }
}
