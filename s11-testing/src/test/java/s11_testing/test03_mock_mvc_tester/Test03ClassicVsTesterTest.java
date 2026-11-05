package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 1 lekcji Test03MockMvcTester: ta sama asercja zapisana klasycznie (MockMvc + andExpect) i przez
 * MockMvcTester (asercje AssertJ). Obie strony są w {@code @WebMvcTest} i działają na tym samym kontrolerze.
 */
@WebMvcTest(ProductController.class)
@Import(ProductService.class)   // prawdziwy serwis (w pamięci) — wycinek WebMvc go nie tworzy
class Test03ClassicVsTesterTest {

    @Autowired
    MockMvc mockMvc;           // klasyczny: perform(...).andExpect(...)

    @Autowired
    MockMvcTester mvc;         // nowy (Spring 6.2+): assertThat(mvc.get().uri(...))...; @WebMvcTest tworzy oba

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("Sekcja 1: klasycznie — MockMvc.perform(...).andExpect(...) (wyjątek throws Exception w sygnaturze)")
    void classic() throws Exception {
        mockMvc.perform(get("/test03/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"));
    }

    @Test
    @DisplayName("Sekcja 1: ta sama asercja przez MockMvcTester — bez throws Exception, w stylu AssertJ")
    void tester() {
        assertThat(mvc.get().uri("/test03/products/1"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa");
    }

    @Test
    @DisplayName("Sekcja 1: oba style zgłaszają nieudaną asercję jako AssertionError z oczekiwanym i faktycznym kodem")
    void bothReportFailures() {
        assertThatThrownBy(() -> mockMvc.perform(get("/test03/products/1")).andExpect(status().isNotFound()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("404").hasMessageContaining("200");

        assertThatThrownBy(() -> assertThat(mvc.get().uri("/test03/products/1")).hasStatus(404))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("404").hasMessageContaining("200");
    }
}
