package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Err03ControllerAdvice. {@code @WebMvcTest} bez listy kontrolerów ładuje WSZYSTKIE kontrolery lekcji
 * oraz wszystkie beany {@code @ControllerAdvice} — dokładnie tego potrzebujemy (kontrolery + advice, bez serwisów).
 */
@WebMvcTest
class Err03ControllerAdviceTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Jeden globalny advice obsługuje wyjątek z kontrolera, który sam nic nie obsługuje: 404 + JSON")
    void globalAdviceHandlesException() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.handledBy").value("global"))
                .andExpect(jsonPath("$.error").value("Nie znaleziono produktu o id 999"));
    }

    @Test
    @DisplayName("2. Catch-all (Exception) daje 500 i STAŁY komunikat — szczegóły wyjątku nie wychodzą do klienta")
    void catchAllHidesDetails() throws Exception {
        mockMvc.perform(get("/api/products/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRICE_V3"))));
    }

    @Test
    @DisplayName("3. Lokalny @ExceptionHandler z kontrolera wygrywa z globalnym advice dla tego samego wyjątku")
    void localBeatsGlobal() throws Exception {
        mockMvc.perform(get("/api/local/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.handledBy").value("local"));
    }

    @Test
    @DisplayName("4. Dwa advice obsługują ten sam wyjątek: wygrywa ten z mniejszą liczbą w @Order (PaymentAdvice, nie global)")
    void orderDecidesBetweenAdvices() throws Exception {
        mockMvc.perform(get("/api/products/pay"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.handledBy").value("payment-advice"));
    }

    @Test
    @DisplayName("5. Zakres: OrderAdvice (assignableTypes) działa w OrderController, ale nie w ProductController")
    void scopeLimitsAdvice() throws Exception {
        mockMvc.perform(get("/api/orders/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.handledBy").value("order-advice"));

        // ten sam typ wyjątku w kontrolerze spoza zakresu → trafia do GlobalExceptionHandler (catch-all Exception)
        mockMvc.perform(get("/api/products/conflict"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.handledBy").value("global"));
    }

    @Test
    @DisplayName("6. PUŁAPKA: pierwszy advice (wg @Order) z PASUJĄCYM handlerem wygrywa, choćby szeroki — nie szuka się najbardziej szczegółowego we wszystkich")
    void firstMatchingAdviceWinsEvenIfBroad() throws Exception {
        // Global ma szczegółowy handler ProductNotFoundException (404), ale OrderAdvice (@Order(1)) ma RuntimeException
        mockMvc.perform(get("/api/orders/product"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.handledBy").value("order-advice-fallback"));
        mockMvc.perform(get("/api/orders/pay"))
                .andExpect(status().isServiceUnavailable());   // PaymentAdvice też przegrywa z OrderAdvice w tym kontrolerze
    }

    @Test
    @DisplayName("1b. PUŁAPKA catch-all: POST na adres tylko-GET (powinno być 405) zamienia się w 500, bo Exception.class łapie wszystko")
    void catchAllSwallowsMvcErrors() throws Exception {
        mockMvc.perform(post("/api/products/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.handledBy").value("global"));
    }
}
