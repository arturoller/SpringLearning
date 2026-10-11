package s06_error_handling.solutions.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Err02ExceptionHandler. @Tag("wzorzec") — uruchamiasz sam (▶ obok klasy).
 * Kontrolery ćwiczeń testujemy bez Springa: {@code MockMvcBuilders.standaloneSetup(kontroler)} (samodzielna konfiguracja
 * MockMvc dla jednego obiektu) — szybko i bez skanowania.
 */
@Tag("wzorzec")
class Err02ExceptionHandlerSolutionsTest {

    @Test
    @DisplayName("Ćwiczenie 1: wyjątek → kod HTTP")
    void exercise1() {
        assertThat(Err02ExceptionHandlerSolutions.solution1(new IllegalArgumentException())).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Err02ExceptionHandlerSolutions.solution1(new NumberFormatException())).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Err02ExceptionHandlerSolutions.solution1(new NoSuchElementException())).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Err02ExceptionHandlerSolutions.solution1(new IllegalStateException())).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Ćwiczenie 2: ResponseStatusException w kontrolerze")
    void exercise2() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Err02ExceptionHandlerSolutions.Solution2Controller()).build();

        mvc.perform(get("/api/ex/items/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ex/items/101")).andExpect(status().isNotFound());
        mvc.perform(get("/api/ex/items/5")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(5));
        assertThat(mvc.perform(get("/api/ex/items/101")).andReturn().getResponse().getErrorMessage()).isEqualTo("Brak przedmiotu");
    }

    @Test
    @DisplayName("Ćwiczenie 3: lokalny @ExceptionHandler zamienia OrderNotFoundException na 404 z JSON-em")
    void exercise3() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Err02ExceptionHandlerSolutions.Solution3Controller()).build();

        mvc.perform(get("/api/ex/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Nie znaleziono zamówienia o id 404"));
        mvc.perform(get("/api/ex/orders/7")).andExpect(status().isOk());
    }
}
