package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Rest01Controllers. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Dopóki ćwiczenie nie jest rozwiązane, test pada: UnsupportedOperationException: TODO albo 404 (brak mapowania).
 */
@Tag("cwiczenie")
@WebMvcTest(controllers = {Rest01ExerciseController.class, ProductController.class})
class Rest01ControllersExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RequestMappingHandlerMapping mapping;

    @Test
    @DisplayName("Ćwiczenie 1: GET /api/cw01/ping → pong")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/cw01/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: GET /api/cw01/categories → tablica JSON kategorii (alfabetycznie)")
    void exercise2() throws Exception {
        mockMvc.perform(get("/api/cw01/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        ["DOM","ELEKTRONIKA","KSIAZKI","ODZIEZ","SPOZYWCZE"]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Ćwiczenie 3: jeden adres /api/cw01/cart — GET i DELETE działają, POST → 405")
    void exercise3() throws Exception {
        mockMvc.perform(get("/api/cw01/cart"))
                .andExpect(status().isOk())
                .andExpect(content().string("Koszyk: 0 produktów"));
        mockMvc.perform(delete("/api/cw01/cart"))
                .andExpect(status().isOk())
                .andExpect(content().string("Koszyk wyczyszczony"));
        mockMvc.perform(post("/api/cw01/cart"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Ćwiczenie 4: spis endpointów ProductController z RequestMappingHandlerMapping")
    void exercise4() {
        assertThat(Rest01ExerciseController.describeEndpoints(mapping, ProductController.class)).containsExactly(
                "DELETE /api/products/{sku}",
                "GET /api/products",
                "GET /api/products/{sku}",
                "PATCH /api/products/{sku}/stock",
                "POST /api/products",
                "PUT /api/products/{sku}");
    }
}
