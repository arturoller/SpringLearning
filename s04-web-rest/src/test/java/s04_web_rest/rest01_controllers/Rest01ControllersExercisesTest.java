package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest01Controllers. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(CustomerController.class)
class Rest01ControllersExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: GET /api/customers → 200 i tablica imion")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Anna\",\"Bartek\",\"Celina\"]"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: GET /api/customers/count → 3, DELETE na ten adres → 405")
    void exercise2() throws Exception {
        mockMvc.perform(get("/api/customers/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
        mockMvc.perform(delete("/api/customers/count"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Ćwiczenie 3: symulacja DispatcherServlet — 200, 404 albo 405")
    void exercise3() {
        Map<String, Set<String>> routes = Map.of(
                "/api/products", Set.of("GET", "POST"),
                "/api/shop/info", Set.of("GET"),
                "/api/orders", Set.of("POST"));

        assertThat(Rest01Controllers.exercise3(routes, "GET", "/api/products")).isEqualTo(200);
        assertThat(Rest01Controllers.exercise3(routes, "post", "/api/products")).isEqualTo(200);
        assertThat(Rest01Controllers.exercise3(routes, "DELETE", "/api/products")).isEqualTo(405);
        assertThat(Rest01Controllers.exercise3(routes, "GET", "/api/nothing")).isEqualTo(404);
        assertThat(Rest01Controllers.exercise3(routes, "head", "/api/shop/info")).isEqualTo(200);
        assertThat(Rest01Controllers.exercise3(routes, "HEAD", "/api/orders")).isEqualTo(405);
        assertThat(Rest01Controllers.exercise3(routes, "OPTIONS", "/api/orders")).isEqualTo(200);
        assertThat(Rest01Controllers.exercise3(routes, "OPTIONS", "/api/nothing")).isEqualTo(404);
    }
}
