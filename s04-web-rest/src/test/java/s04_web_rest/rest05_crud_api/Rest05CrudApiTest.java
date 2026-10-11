package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest05CrudApi (MockMvc). {@code @WebMvcTest} + prawdziwe repozytorium przez {@code @Import}.
 * Kontekst Springa jest współdzielony między testami (cache), a repozytorium ma stan — dlatego przed KAŻDYM testem
 * przywracamy dane startowe (resetToSampleData). Dzięki temu kolejność testów nie ma znaczenia.
 */
@WebMvcTest(ProductCrudController.class)
@Import(ProductRepository.class)
class Rest05CrudApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void resetData() {
        repository.resetToSampleData();
    }

    @Test
    @DisplayName("1. GET kolekcji → 200 i trzy produkty po kolei; GET jednego → 200; nieznane id → 404")
    void read() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains(1, 2, 3)));
        mockMvc.perform(get("/api/products/2"))
                .andExpect(content().json("{\"id\":2,\"name\":\"Herbata\",\"priceGrosze\":1599}"));
        mockMvc.perform(get("/api/products/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("2. POST → 201, Location i produkt z id 4; POST nie jest idempotentny — drugi taki sam tworzy id 5")
    void create() throws Exception {
        String body = "{\"name\":\"Ciastka\",\"priceGrosze\":1299}";
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/4"))
                .andExpect(content().json("{\"id\":4,\"name\":\"Ciastka\",\"priceGrosze\":1299}"));
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.id").value(5));
        mockMvc.perform(get("/api/products")).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("2. POST z pustą nazwą, ujemną ceną albo bez ceny → 400")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"priceGrosze\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nazwa produktu nie może być pusta"));
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciastka\",\"priceGrosze\":-1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciastka\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. PUT zastępuje całość i jest idempotentny; PUT bez ceny → 400 (pytanie 3); nieznane id → 404")
    void replace() throws Exception {
        String body = "{\"name\":\"Herbata zielona\",\"priceGrosze\":1799}";
        for (int i = 0; i < 3; i++) {   // trzy razy to samo — wynik i stan zawsze te same
            mockMvc.perform(put("/api/products/2").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"id\":2,\"name\":\"Herbata zielona\",\"priceGrosze\":1799}"));
        }
        mockMvc.perform(put("/api/products/2").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Herbata\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/99").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. PATCH zmienia tylko przysłane pola; {\"name\":null} = bez zmian (pytanie 6); nieznane id → 404")
    void patchPartially() throws Exception {
        mockMvc.perform(patch("/api/products/2").contentType(MediaType.APPLICATION_JSON).content("{\"priceGrosze\":1499}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":2,\"name\":\"Herbata\",\"priceGrosze\":1499}"));
        mockMvc.perform(patch("/api/products/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":null}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"name\":\"Kawa\",\"priceGrosze\":2999}"));
        mockMvc.perform(patch("/api/products/1").contentType(MediaType.APPLICATION_JSON).content("{\"priceGrosze\":-5}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/products/99").contentType(MediaType.APPLICATION_JSON).content("{\"priceGrosze\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. DELETE → 204; drugi raz → 404, a stan ten sam: produktu nie ma (pytanie 2)")
    void deleteIsIdempotent() throws Exception {
        mockMvc.perform(delete("/api/products/3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete("/api/products/3")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/3")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products")).andExpect(jsonPath("$.length()").value(2));
    }
}
