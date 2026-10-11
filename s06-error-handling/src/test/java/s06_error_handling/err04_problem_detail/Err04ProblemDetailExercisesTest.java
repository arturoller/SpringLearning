package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Err04ProblemDetail. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Err04ProblemDetailExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: ProblemDetail dla nieistniejącego produktu")
    void exercise1() {
        ProblemDetail problem = Err04ProblemDetail.exercise1(42);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Produkt nie istnieje");
        assertThat(problem.getDetail()).isEqualTo("Nie znaleziono produktu o id 42");
        assertThat(problem.getType()).hasToString("https://shop.example.com/problems/product-not-found");
        assertThat(problem.getProperties()).containsEntry("productId", 42L);
    }

    @Test
    @DisplayName("Ćwiczenie 2: ErrorResponseException z ProblemDetail dla limitu zapytań")
    void exercise2() {
        ErrorResponseException exception = Err04ProblemDetail.exercise2(30);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exception.getBody().getTitle()).isEqualTo("Zbyt wiele zapytań");
        assertThat(exception.getBody().getDetail()).isEqualTo("Limit zapytań przekroczony");
        assertThat(exception.getBody().getProperties()).containsEntry("retryAfterSeconds", 30);
    }

    @Test
    @DisplayName("Ćwiczenie 3: advice z ResponseEntityExceptionHandler — IllegalArgumentException → 400 problem+json, 405 w prezencie")
    void exercise3() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new Err04ProblemDetail.Exercise3Advice())
                .build();

        mvc.perform(get("/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Niepoprawne dane"))
                .andExpect(jsonPath("$.detail").value("Zły rabat"));
        mvc.perform(post("/bad"))   // odziedziczone po ResponseEntityExceptionHandler
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /** Kontroler pomocniczy testu (zagnieżdżona klasa testu — Spring jej nie skanuje). */
    @RestController
    static class FailingController {

        @GetMapping("/bad")
        String bad() {
            throw new IllegalArgumentException("Zły rabat");
        }
    }
}
