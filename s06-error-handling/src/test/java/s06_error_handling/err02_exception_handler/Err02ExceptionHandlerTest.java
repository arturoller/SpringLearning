package s06_error_handling.err02_exception_handler;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Err02ExceptionHandler. Wybieramy {@code @WebMvcTest} (wycinek web): {@code @ExceptionHandler},
 * {@code @ResponseStatus} i {@code ResponseStatusException} obsługuje DispatcherServlet, więc MockMvc je widzi
 * (w przeciwieństwie do /error z Err01DefaultErrors).
 */
@WebMvcTest({CatalogController.class, OrderController.class, InventoryController.class})
class Err02ExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Wyjątek z @ResponseStatus → 404; powód (reason) trafia do sendError, a nie do ciała JSON")
    void responseStatusOnException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/catalog/999")).andExpect(status().isNotFound()).andReturn();

        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Nie ma takiego produktu");   // sendError(404, powód)
        assertThat(result.getResponse().getContentAsString()).isEmpty();   // ciało zrobi dopiero /error na prawdziwym serwerze
    }

    @Test
    @DisplayName("2. ResponseStatusException(409, powód) → status 409 bez osobnej klasy wyjątku")
    void responseStatusException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/catalog/5/stock")).andExpect(status().isConflict()).andReturn();

        assertThat(result.getResponse().getErrorMessage()).isEqualTo("Towar chwilowo niedostępny: 5");
    }

    @Test
    @DisplayName("3. Lokalny @ExceptionHandler zamienia IllegalArgumentException na 400 i bezpieczne ciało JSON")
    void localExceptionHandler() throws Exception {
        mockMvc.perform(get("/api/catalog/discount").param("percent", "150"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Niepoprawne dane: Rabat musi być w zakresie 0..100: 150"));
    }

    @Test
    @DisplayName("4. Wygrywa NAJBARDZIEJ SZCZEGÓŁOWY typ wyjątku: NumberFormatException trafia do handlera IllegalArgumentException, a nie RuntimeException")
    void mostSpecificHandlerWins() throws Exception {
        mockMvc.perform(get("/api/inventory/parse").param("value", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Niepoprawne dane: For input string: \"abc\""));

        mockMvc.perform(get("/api/inventory/broken"))   // IllegalStateException → tylko handler RuntimeException pasuje
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Wystąpił nieoczekiwany błąd"));
    }

    @Test
    @DisplayName("4b. PUŁAPKA: szeroki handler RuntimeException połyka też wyjątek z @ResponseStatus(404) → wychodzi 500")
    void broadHandlerSwallowsResponseStatusException() throws Exception {
        mockMvc.perform(get("/api/inventory/missing"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Wystąpił nieoczekiwany błąd"));
    }

    @Test
    @DisplayName("5. @ExceptionHandler w kontrolerze wygrywa z @ResponseStatus na klasie wyjątku: 404 zamiast 410")
    void handlerBeatsResponseStatusAnnotation() throws Exception {
        mockMvc.perform(get("/api/catalog/13"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Produkt 13 został wycofany ze sprzedaży"));
    }

    @Test
    @DisplayName("6. @ExceptionHandler jest lokalny: w OrderController działa jego własny, a cudzy (z CatalogController) nie")
    void handlersAreLocalToController() throws Exception {
        mockMvc.perform(get("/api/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Nie znaleziono zamówienia o id 404"));

        // IllegalArgumentException w OrderController: brak handlera → wyjątek wylatuje (MockMvc), na serwerze byłoby 500
        assertThatThrownBy(() -> mockMvc.perform(get("/api/orders/invalid")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
