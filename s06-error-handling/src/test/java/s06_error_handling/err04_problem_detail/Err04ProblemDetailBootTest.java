package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2: jedna linijka {@code spring.mvc.problemdetails.enabled=true} włącza w Boot 4 gotowy advice
 * (ProblemDetailsExceptionHandler, dziedziczy po ResponseEntityExceptionHandler). Własnego advice lekcji NIE włączamy.
 */
@WebMvcTest(value = ProductController.class, properties = "spring.mvc.problemdetails.enabled=true")
class Err04ProblemDetailBootTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("2. problemdetails.enabled=true: brak wymaganego parametru → 400 i application/problem+json")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Required parameter 'q' is not present."))
                .andExpect(jsonPath("$.instance").value("/api/search"))
                .andExpect(jsonPath("$.type").doesNotExist());   // about:blank jest wartością domyślną i w JSON-ie go nie ma
    }

    @Test
    @DisplayName("2. Zła metoda HTTP → 405 jako problem+json; ResponseStatusException też staje się problem+json")
    void wrongMethodAndResponseStatusException() throws Exception {
        mockMvc.perform(post("/api/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.detail").value("Method 'POST' is not supported."));

        mockMvc.perform(get("/api/products/1/stock"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Towar chwilowo niedostępny: 1"));
    }

    @Test
    @DisplayName("4. ErrorResponseException z własnym ProblemDetail: tytuł, typ i własne pole retryAfterSeconds trafiają do JSON-a")
    void errorResponseException() throws Exception {
        mockMvc.perform(get("/api/limits"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Zbyt wiele zapytań"))
                .andExpect(jsonPath("$.type").value("https://shop.example.com/problems/rate-limit"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }

    @Test
    @DisplayName("2. Wyjątki domenowe nadal wymagają własnego handlera — gotowy advice Boota zna tylko wyjątki Springa")
    void domainExceptionStillUnhandled() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/products/999"))).hasCauseInstanceOf(ProductNotFoundException.class);
    }
}
