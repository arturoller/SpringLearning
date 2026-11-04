package s06_error_handling.err05_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import s06_error_handling.err05_custom_exceptions.Err05Advice;
import s06_error_handling.err05_custom_exceptions.OrderService;
import s06_error_handling.err05_custom_exceptions.ShopController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Err05CustomExceptions — wyjątek oznaczony {@code @ResponseStatus} (SupplierNotFoundException).
 * Ten test: bez żadnego advice: adnotacja działa, ale ciało odpowiedzi jest PUSTE (MockMvc) — format błędu poza naszą kontrolą.
 */
@WebMvcTest
@ContextConfiguration(classes = {ShopController.class, OrderService.class, Err05Advice.class})
class Err05ResponseStatusWithCatchAllTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 2 (pułapka): handler Exception.class w advice przesłania @ResponseStatus — zamiast 404 klient dostaje 500")
    void responseStatusOnException() throws Exception {
        mockMvc.perform(get("/err05/suppliers/3"))
                .andExpect(status().isInternalServerError())          // adnotacja @ResponseStatus została zignorowana
                .andExpect(jsonPath("$.code").value("UNEXPECTED"));
    }
}
