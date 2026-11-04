package s04_web_rest.rest02_request_data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest02RequestData. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ExerciseController.class)
class Rest02RequestDataExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: dwie zmienne ścieżki")
    void exercise1() throws Exception {
        mockMvc.perform(get("/rest02/cw/customers/7/orders/12"))
                .andExpect(content().string("klient 7, zamówienie 12"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: parametr z wartością domyślną")
    void exercise2() throws Exception {
        mockMvc.perform(get("/rest02/cw/greet").param("name", "Ola")).andExpect(content().string("Cześć, Ola!"));
        mockMvc.perform(get("/rest02/cw/greet")).andExpect(content().string("Cześć, gościu!"));
    }

    @Test
    @DisplayName("Ćwiczenie 3: wymagany nagłówek X-Lang")
    void exercise3() throws Exception {
        mockMvc.perform(get("/rest02/cw/lang").header("X-Lang", "pl")).andExpect(content().string("język: pl"));
        mockMvc.perform(get("/rest02/cw/lang")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ćwiczenie 4: JSON z kuponem w ciele zapytania")
    void exercise4() throws Exception {
        mockMvc.perform(post("/rest02/cw/coupon").contentType("application/json")
                        .content("{\"code\":\"ZIMA10\",\"percent\":10}"))
                .andExpect(content().string("kod ZIMA10: -10%"));
    }

    @Test
    @DisplayName("Ćwiczenie 5: lista parametrów n sumowana; zły typ → 400")
    void exercise5() throws Exception {
        mockMvc.perform(get("/rest02/cw/sum").param("n", "1", "2", "3")).andExpect(content().string("6"));
        mockMvc.perform(get("/rest02/cw/sum").param("n", "abc")).andExpect(status().isBadRequest());
    }
}
