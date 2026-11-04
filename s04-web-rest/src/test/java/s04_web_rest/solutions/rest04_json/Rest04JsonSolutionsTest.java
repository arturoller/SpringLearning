package s04_web_rest.solutions.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy rozwiązań wzorcowych lekcji Rest04Json. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest(Rest04SolutionsController.class)
class Rest04JsonSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: pole ownerEmail występuje w JSON jako owner_email")
    void exercise1() throws Exception {
        mockMvc.perform(get("/rest04/cw/gift-card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GC-1"))
                .andExpect(jsonPath("$.owner_email").value("ala@example.com"))
                .andExpect(jsonPath("$.ownerEmail").doesNotExist());
    }

    @Test
    @DisplayName("Wzorzec 2: passwordHash nie występuje w JSON")
    void exercise2() throws Exception {
        mockMvc.perform(get("/rest04/cw/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ala"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Wzorzec 3: description równe null znika z JSON")
    void exercise3() throws Exception {
        mockMvc.perform(get("/rest04/cw/coupon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMO10"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("Wzorzec 4: data faktury w formacie dd.MM.yyyy, kwota jako liczba")
    void exercise4() throws Exception {
        mockMvc.perform(get("/rest04/cw/invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issued").value("31.01.2026"))
                .andExpect(jsonPath("$.total").value(199.90));
    }

    @Test
    @DisplayName("Wzorzec 5: rozmiar paczki w JSON to małe litery (\"large\"), a \"small\" z żądania to stała SMALL")
    void exercise5() throws Exception {
        mockMvc.perform(get("/rest04/cw/parcel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value("large"));
        mockMvc.perform(post("/rest04/cw/parcel").contentType("application/json").content("{\"size\":\"small\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("rozmiar: SMALL"));
    }
}
