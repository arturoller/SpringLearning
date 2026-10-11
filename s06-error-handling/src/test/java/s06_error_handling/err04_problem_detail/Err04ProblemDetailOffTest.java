package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sekcja 2: ustawienia domyślne Boota 4 — {@code spring.mvc.problemdetails.enabled=false}, brak własnego advice. */
@WebMvcTest(ProductController.class)
class Err04ProblemDetailOffTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("2. Domyślnie (problemdetails.enabled=false) błędy MVC mają sam status — bez ciała problem+json")
    void noProblemDetailByDefault() throws Exception {
        MvcResult missingParam = mockMvc.perform(get("/api/search")).andExpect(status().isBadRequest()).andReturn();
        MvcResult wrongMethod = mockMvc.perform(post("/api/search")).andExpect(status().isMethodNotAllowed()).andReturn();

        assertThat(missingParam.getResponse().getContentAsString()).isEmpty();
        assertThat(missingParam.getResponse().getContentType()).isNull();
        assertThat(wrongMethod.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("2. Wyjątek domenowy bez własnej obsługi nie staje się ProblemDetail — wylatuje (na serwerze: 500)")
    void domainExceptionNotHandled() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/products/999"))).hasCauseInstanceOf(ProductNotFoundException.class);
    }
}
