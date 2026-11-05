package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest05CrudApi — idempotencja metod. Tym razem serwis jest PRAWDZIWY (dodany przez {@code @Import}),
 * bo sprawdzamy skutek na danych: ile produktów jest po dwukrotnym wysłaniu tego samego zapytania.
 */
@WebMvcTest(ProductController.class)
@Import(ProductService.class)
class Rest05IdempotencyTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("GET jest idempotentny (i bezpieczny): wielokrotne wywołanie niczego nie zmienia")
    void getIsIdempotent() throws Exception {
        mockMvc.perform(get("/rest05/products/1")).andExpect(status().isOk());
        mockMvc.perform(get("/rest05/products/1")).andExpect(status().isOk());
        mockMvc.perform(get("/rest05/products")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("PUT jest idempotentny: to samo zapytanie dwa razy daje ten sam stan i jednego produktu")
    void putIsIdempotent() throws Exception {
        String body = "{\"name\":\"Kawa ziarnista\",\"price\":39.00}";
        mockMvc.perform(put("/rest05/products/1").contentType("application/json").content(body)).andExpect(status().isOk());
        mockMvc.perform(put("/rest05/products/1").contentType("application/json").content(body)).andExpect(status().isOk());

        mockMvc.perform(get("/rest05/products"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Kawa ziarnista"));
    }

    @Test
    @DisplayName("DELETE jest idempotentny w skutku: za drugim razem kod to 404, ale stan taki sam (produktu nie ma)")
    void deleteIsIdempotent() throws Exception {
        mockMvc.perform(delete("/rest05/products/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/rest05/products/1")).andExpect(status().isNotFound());

        mockMvc.perform(get("/rest05/products")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST NIE jest idempotentny: to samo zapytanie dwa razy tworzy dwa produkty (id 3 i 4)")
    void postIsNotIdempotent() throws Exception {
        String body = "{\"name\":\"Ciastka\",\"price\":12.50}";
        mockMvc.perform(post("/rest05/products").contentType("application/json").content(body))
                .andExpect(jsonPath("$.id").value(3));
        mockMvc.perform(post("/rest05/products").contentType("application/json").content(body))
                .andExpect(jsonPath("$.id").value(4));

        mockMvc.perform(get("/rest05/products")).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("PATCH z ustawieniem ceny (\"ustaw 19.90\") jest idempotentny, ale PATCH \"obniż o 10%\" by nie był")
    void patchSettingAbsoluteValueIsIdempotent() throws Exception {
        String body = "{\"price\":19.90}";
        mockMvc.perform(patch("/rest05/products/1").contentType("application/json").content(body))
                .andExpect(jsonPath("$.price").value(19.90));
        mockMvc.perform(patch("/rest05/products/1").contentType("application/json").content(body))
                .andExpect(jsonPath("$.price").value(19.90));
    }
}
