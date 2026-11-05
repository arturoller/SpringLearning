package s06_error_handling.err03_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import s06_error_handling.err03_controller_advice.ProductController;
import s06_error_handling.err03_controller_advice.ShopAdvice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 4 lekcji Err03ControllerAdvice — pułapka kolejności: ogólny advice z {@code @Order(HIGHEST_PRECEDENCE)} wyprzedza
 * szczegółowy {@code ShopAdvice}.
 */
@WebMvcTest
@ContextConfiguration(classes = {ProductController.class, ShopAdvice.class, GreedyAdvice.class})
class Err03GreedyOrderTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 4 (pułapka): ogólny advice o najwyższym pierwszeństwie wyprzedza szczegółowy — zamiast 404 jest 500")
    void generalAdviceWithHigherPrecedenceWins() throws Exception {
        mockMvc.perform(get("/err03/products/9"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("zachlanny-500"));
    }
}
