package s04_web_rest.solutions.rest05_crud_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Rest05CrudApi. @Tag("wzorzec") — uruchamiasz sam (▶ obok klasy).
 * {@code @DirtiesContext} (brudny kontekst) po każdym teście — kontroler ma stan (mapę klientów), więc każdy test
 * dostaje świeży kontekst z danymi startowymi.
 */
@Tag("wzorzec")
@WebMvcTest(SolutionCustomerController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Rest05CrudApiSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: GET klienta → 200; nieznane id → 404")
    void solution1() throws Exception {
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"name\":\"Anna\",\"email\":\"anna@example.com\"}"));
        mockMvc.perform(get("/api/customers/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Wzorzec 2: POST → 201 + Location + klient z id 3; pusta nazwa → 400")
    void solution2() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Celina\",\"email\":\"celina@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/3"))
                .andExpect(content().json("{\"id\":3,\"name\":\"Celina\",\"email\":\"celina@example.com\"}"));
        mockMvc.perform(get("/api/customers/3")).andExpect(status().isOk());
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"email\":\"x@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Wzorzec 3: PATCH zmienia tylko przysłane pola; DELETE → 204, potem 404")
    void solution3() throws Exception {
        mockMvc.perform(patch("/api/customers/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bartek@sklep.example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":2,\"name\":\"Bartek\",\"email\":\"bartek@sklep.example.com\"}"));
        mockMvc.perform(patch("/api/customers/99").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/customers/2")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/customers/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/customers/2")).andExpect(status().isNotFound());
    }
}
