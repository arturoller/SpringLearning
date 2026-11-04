package s06_error_handling.err03_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import s06_error_handling.err03_controller_advice.ProductController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 4 lekcji Err03ControllerAdvice — pułapka "łap wszystko". Kontekst budujemy ręcznie
 * ({@code @ContextConfiguration(classes = …)}): tylko kontroler i zły advice, bez prawdziwego {@code FallbackAdvice} z lekcji.
 */
@WebMvcTest
@ContextConfiguration(classes = {ProductController.class, NaiveCatchAllAdvice.class})
class Err03NaiveCatchAllTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 4 (pułapka): naiwne Exception.class zamienia 405 (zła metoda HTTP) na 500")
    void methodNotAllowedBecomes500() throws Exception {
        mockMvc.perform(delete("/err03/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("naiwny-500"));   // bez advice byłoby 405 z nagłówkiem Allow
    }

    @Test
    @DisplayName("Sekcja 4 (pułapka): naiwne Exception.class zamienia 404 (nieznana ścieżka) i 400 (zły typ id) na 500")
    void notFoundAndBadRequestBecome500() throws Exception {
        mockMvc.perform(get("/err03/nie-ma-takiej-sciezki")).andExpect(status().isInternalServerError());
        mockMvc.perform(get("/err03/products/abc")).andExpect(status().isInternalServerError());
    }
}
