package s04_web_rest.rest03_responses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest03Responses. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ExerciseController.class)
class Rest03ResponsesExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: 200 dla istniejącego zamówienia, 404 dla pozostałych")
    void exercise1() throws Exception {
        mockMvc.perform(get("/rest03/cw/orders/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("zamówienie 1"));
        mockMvc.perform(get("/rest03/cw/orders/2")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ćwiczenie 2: usunięcie → 204 bez ciała")
    void exercise2() throws Exception {
        mockMvc.perform(delete("/rest03/cw/orders/5"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Ćwiczenie 3: utworzenie → 201 z nagłówkiem Location")
    void exercise3() throws Exception {
        mockMvc.perform(post("/rest03/cw/orders"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/rest03/cw/orders/42"))
                .andExpect(content().string("utworzono"));
    }

    @Test
    @DisplayName("Ćwiczenie 4: płatność odrzucona → 402 dzięki adnotacji na wyjątku")
    void exercise4() throws Exception {
        mockMvc.perform(post("/rest03/cw/orders/7/pay")).andExpect(content().string("opłacono"));
        mockMvc.perform(post("/rest03/cw/orders/13/pay")).andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("Ćwiczenie 5: własny nagłówek i Cache-Control: no-store")
    void exercise5() throws Exception {
        mockMvc.perform(get("/rest03/cw/report"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string("raport"));
    }
}
