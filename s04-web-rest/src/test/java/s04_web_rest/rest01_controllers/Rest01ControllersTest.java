package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest01Controllers. {@code @WebMvcTest} (bez listy klas) = wycinek web z WSZYSTKIMI kontrolerami
 * lekcji — wystarczy, bo sprawdzamy tylko mapowania i odpowiedzi, a kontrolery nie mają zależności.
 */
@WebMvcTest
class Rest01ControllersTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. @RestController: GET /api/products → 200, JSON z wynikiem metody all()")
    void restControllerReturnsJson() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].handler").value("ProductController.all"))
                .andExpect(jsonPath("$[0].info").value("lista produktów"));
    }

    @Test
    @DisplayName("2. @Controller + @ResponseBody zwraca JSON jak @RestController")
    void controllerWithResponseBody() throws Exception {
        mockMvc.perform(get("/api/shop/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handler").value("ShopInfoController.info"));
    }

    @Test
    @DisplayName("2. @Controller BEZ @ResponseBody: String to nazwa widoku — puste ciało i forward do „shop-hours”")
    void controllerWithoutResponseBodyForwardsToView() throws Exception {
        mockMvc.perform(get("/api/shop/forgotten"))
                .andExpect(status().isOk())                 // w MockMvc 200 — prawdziwy serwer da 404 (ServerTest)
                .andExpect(forwardedUrl("shop-hours"))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("3. Ten sam adres /api/products/7 — inna metoda HTTP, inna metoda Javy")
    void httpMethodSelectsHandler() throws Exception {
        mockMvc.perform(get("/api/products/7")).andExpect(jsonPath("$.handler").value("ProductController.one"));
        mockMvc.perform(put("/api/products/7")).andExpect(jsonPath("$.handler").value("ProductController.replace"));
        mockMvc.perform(patch("/api/products/7")).andExpect(jsonPath("$.handler").value("ProductController.patch"));
        mockMvc.perform(delete("/api/products/7")).andExpect(jsonPath("$.handler").value("ProductController.delete"));
        mockMvc.perform(post("/api/products")).andExpect(jsonPath("$.handler").value("ProductController.create"));
    }

    @Test
    @DisplayName("3. Konkretna ścieżka /bestsellers wygrywa ze wzorcem /{id} (pytanie 3)")
    void specificPathWins() throws Exception {
        mockMvc.perform(get("/api/products/bestsellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handler").value("ProductController.bestsellers"));
    }

    @Test
    @DisplayName("4. Adres znany, metoda HTTP nie → 405 i nagłówek Allow z dozwolonymi metodami (pytanie 2)")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/shop/info"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"));
        mockMvc.perform(delete("/api/shop/info"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"));
        // kolejność metod w Allow nie jest ustalona — sprawdzamy, że nagłówek ZAWIERA każdą z nich
        mockMvc.perform(post("/api/products/5"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", allOf(containsString("GET"), containsString("PUT"),
                        containsString("PATCH"), containsString("DELETE"))));
    }

    @Test
    @DisplayName("4. OPTIONS za darmo: 200 i Allow: GET,HEAD,OPTIONS")
    void optionsForFree() throws Exception {
        mockMvc.perform(options("/api/shop/info"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", allOf(containsString("GET"), containsString("HEAD"),
                        containsString("OPTIONS"))));
    }

    @Test
    @DisplayName("5. Nieznany adres → 404 Not Found")
    void unknownPath() throws Exception {
        mockMvc.perform(get("/api/nothing")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/product")).andExpect(status().isNotFound());   // literówka: brak „s”
    }
}
