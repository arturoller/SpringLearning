package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Sekcja 4 lekcji Err05CustomExceptions: co trafia do logu. {@code OutputCaptureExtension} (output capture = przechwytywanie
 * wyjścia) zbiera tekst wypisany na konsolę, więc możemy sprawdzić poziom i to, czy jest ślad stosu (linie zaczynające się od "at ").
 */
@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(ShopController.class)
class Err05LoggingTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService service;

    @Test
    @DisplayName("Sekcja 4: błąd 4xx → jedna linia INFO z kodem, BEZ śladu stosu")
    void clientErrorIsOneInfoLine(CapturedOutput output) throws Exception {
        given(service.getProduct(9)).willThrow(new ProductNotFoundException(9));

        mockMvc.perform(get("/err05/products/9"));

        assertThat(output.getAll()).contains("INFO").contains("404 PRODUCT_NOT_FOUND");
        assertThat(output.getAll()).doesNotContain("ERROR").doesNotContain("\tat s06_error_handling");
    }

    @Test
    @DisplayName("Sekcja 4: błąd 5xx → poziom ERROR i PEŁNY ślad stosu (z nazwą wyjątku i klasą kontrolera)")
    void serverErrorHasStackTrace(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/err05/crash"));

        assertThat(output.getAll()).contains("ERROR").contains("IllegalStateException").contains("\tat s06_error_handling.err05_custom_exceptions.ShopController");
    }
}
