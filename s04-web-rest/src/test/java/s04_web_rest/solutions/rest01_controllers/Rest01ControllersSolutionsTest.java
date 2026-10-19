package s04_web_rest.solutions.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import s04_web_rest.rest01_controllers.ProductController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprawdzenia z Rest01ControllersExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * ProductController lekcji dokładamy przez {@code @Import}, bo leży poza pakietem skanowanym przez
 * Rest01SolutionsApplication (potrzebny w rozwiązaniu 4).
 */
@Tag("wzorzec")
@WebMvcTest(Rest01SolutionController.class)
@Import(ProductController.class)
class Rest01ControllersSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RequestMappingHandlerMapping mapping;

    @Test
    @DisplayName("Wzorzec 1: GET /api/cw01/ping → pong")
    void solution1() throws Exception {
        mockMvc.perform(get("/api/cw01/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    @DisplayName("Wzorzec 2: GET /api/cw01/categories → tablica JSON kategorii (alfabetycznie)")
    void solution2() throws Exception {
        mockMvc.perform(get("/api/cw01/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        ["DOM","ELEKTRONIKA","KSIAZKI","ODZIEZ","SPOZYWCZE"]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Wzorzec 3: jeden adres /api/cw01/cart — GET i DELETE działają, POST → 405")
    void solution3() throws Exception {
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
    @DisplayName("Wzorzec 4: spis endpointów ProductController z RequestMappingHandlerMapping")
    void solution4() {
        assertThat(Rest01SolutionController.describeEndpoints(mapping, ProductController.class)).containsExactly(
                "DELETE /api/products/{sku}",
                "GET /api/products",
                "GET /api/products/{sku}",
                "PATCH /api/products/{sku}/stock",
                "POST /api/products",
                "PUT /api/products/{sku}");
    }
}
