package s04_web_rest.rest02_request_data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Rest02RequestData. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Nierozwiązane ćwiczenie pada z UnsupportedOperationException: TODO (albo 404, gdy brakuje mapowania).
 */
@Tag("cwiczenie")
@WebMvcTest(Rest02ExerciseController.class)
class Rest02RequestDataExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: GET /api/cw02/greeting/Ola → Dzień dobry, Ola!")
    void exercise1() throws Exception {
        String body = mockMvc.perform(get("/api/cw02/greeting/Ola"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).isEqualTo("Dzień dobry, Ola!");
    }

    @Test
    @DisplayName("Ćwiczenie 2: cena brutto z parametrów; vat domyślnie 23; brak net → 400")
    void exercise2() throws Exception {
        mockMvc.perform(get("/api/cw02/gross").param("net", "100.00").param("vat", "8"))
                .andExpect(status().isOk())
                .andExpect(content().string("108.00"));
        mockMvc.perform(get("/api/cw02/gross").param("net", "100.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("123.00"));
        mockMvc.perform(get("/api/cw02/gross").param("net", "99.99"))
                .andExpect(content().string("122.99"));
        mockMvc.perform(get("/api/cw02/gross"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ćwiczenie 3: suma koszyka z ciała JSON; nieznany sku → 400")
    void exercise3() throws Exception {
        mockMvc.perform(post("/api/cw02/cart-total").contentType(MediaType.APPLICATION_JSON).content("""
                        {"lines":[{"sku":"KSI-001","quantity":2},{"sku":"SPO-002","quantity":4}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string("187.96"));
        mockMvc.perform(post("/api/cw02/cart-total").contentType(MediaType.APPLICATION_JSON).content("""
                        {"lines":[{"sku":"XXX-001","quantity":1}]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ćwiczenie 4: wyszukiwarka z @ModelAttribute — warunki łączone, brak parametru = brak warunku")
    void exercise4() throws Exception {
        mockMvc.perform(get("/api/cw02/search").param("text", "kaw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("DOM-001", "SPO-001")));
        mockMvc.perform(get("/api/cw02/search").param("category", "KSIAZKI").param("maxPrice", "100"))
                .andExpect(jsonPath("$", contains("KSI-001", "KSI-003")));
        mockMvc.perform(get("/api/cw02/search").param("text", "kaw").param("category", "DOM").param("maxPrice", "2000"))
                .andExpect(jsonPath("$", contains("DOM-001")));
        mockMvc.perform(get("/api/cw02/search"))
                .andExpect(jsonPath("$", hasSize(14)));
        mockMvc.perform(get("/api/cw02/search").param("category", "ksiazki"))
                .andExpect(status().isBadRequest());
    }
}
