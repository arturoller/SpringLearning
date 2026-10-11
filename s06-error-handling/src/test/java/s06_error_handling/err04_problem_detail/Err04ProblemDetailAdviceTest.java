package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 3–5: własny advice lekcji dziedziczący po ResponseEntityExceptionHandler.
 * {@code err04.custom-advice=true} włącza go; {@code spring.mvc.problemdetails.enabled} zostaje domyślne (false) —
 * własny advice działa SAM, bez tej właściwości.
 */
@WebMvcTest(value = ProductController.class, properties = "err04.custom-advice=true")
class Err04ProblemDetailAdviceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("3. Wyjątek domenowy → ProblemDetail ze wszystkimi polami: type, title, status, detail, instance i własne productId")
    void domainExceptionAsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://shop.example.com/problems/product-not-found"))
                .andExpect(jsonPath("$.title").value("Produkt nie istnieje"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Nie znaleziono produktu o id 999"))
                .andExpect(jsonPath("$.instance").value("/api/products/999"))   // instance uzupełnia Spring: adres żądania
                .andExpect(jsonPath("$.productId").value(999));                  // własne pole na głównym poziomie JSON-a
    }

    @Test
    @DisplayName("4. Dziedziczenie po ResponseEntityExceptionHandler daje problem+json także dla wyjątków Springa MVC")
    void inheritedMvcHandling() throws Exception {
        mockMvc.perform(post("/api/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"));
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("4. ErrorResponseException obsłużony przez advice zachowuje własny ProblemDetail (tytuł, typ, pole)")
    void errorResponseExceptionKeepsBody() throws Exception {
        mockMvc.perform(get("/api/limits"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Zbyt wiele zapytań"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }

    @Test
    @DisplayName("5. Własny advice zastępuje gotowy advice Boota: w kontekście jest dokładnie jeden bean ResponseEntityExceptionHandler")
    void onlyOneResponseEntityExceptionHandler() {
        assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class).values())
                .singleElement().isInstanceOf(ProblemDetailAdvice.class);
    }
}
