package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 3 lekcji Err04ProblemDetail: bez właściwości {@code spring.mvc.problemdetails.enabled} (domyślnie {@code false}, Boot 4.1.1)
 * wbudowany handler nie istnieje. Statusy te same, ale w MockMvc ciała odpowiedzi są PUSTE (MockMvc nie przechodzi
 * przez {@code /error}; na prawdziwym serwerze byłby tam ogólny JSON Boota z Err01DefaultErrors).
 */
@WebMvcTest
@ContextConfiguration(classes = ShopController.class)
class Err04BuiltInOffTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 3: domyślnie wbudowanego handlera nie ma, a 405 i ErrorResponse mają puste ciało")
    void noBuiltInHandlerByDefault() throws Exception {
        assertThat(context.containsBean("problemDetailsExceptionHandler")).isFalse();
        mockMvc.perform(delete("/err04/products/1")).andExpect(status().isMethodNotAllowed()).andExpect(content().string(""));
        mockMvc.perform(get("/err04/products/1/reserve")).andExpect(status().isConflict()).andExpect(content().string(""));
        mockMvc.perform(get("/err04/products/1/archive")).andExpect(status().isGone()).andExpect(content().string(""));
    }
}
