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
 * Sekcja 5 lekcji Err05CustomExceptions — pułapka: łapanie wyjątku w kontrolerze i zwracanie 200 z {@code {"error": …}}.
 * Kontekst budujemy ręcznie: zły kontroler, dobry kontroler i advice z lekcji, żeby porównać odpowiedzi.
 */
@WebMvcTest
@ContextConfiguration(classes = {CatchingController.class, ShopController.class, OrderService.class, Err05Advice.class})
class Err05CatchingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 5 (pułapka): ten sam błąd — zły kontroler daje 200 z polem error, dobry daje 404 ProblemDetail")
    void catchingHidesTheFailure() throws Exception {
        mockMvc.perform(get("/err05/bad/products/9"))
                .andExpect(status().isOk())                          // klient, cache, monitoring i testy widzą "sukces"
                .andExpect(jsonPath("$.error").value("Nie ma produktu o id 9"));
        mockMvc.perform(get("/err05/products/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
