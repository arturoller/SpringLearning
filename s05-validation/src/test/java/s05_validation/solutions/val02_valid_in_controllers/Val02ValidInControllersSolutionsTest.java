package s05_validation.solutions.val02_valid_in_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprawdzenia z Val02ValidInControllersExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@WebMvcTest(SolutionController.class)
class Val02ValidInControllersSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: zły JSON zamówienia → 400, dobry → 200")
    void exercise1() throws Exception {
        mockMvc.perform(post("/exercise/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/exercise/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"Ala\",\"quantity\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Wzorzec 2: id >= 1 i quantity w 1..99, inaczej 400")
    void exercise2() throws Exception {
        mockMvc.perform(get("/exercise/orders/0").param("quantity", "5")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/exercise/orders/1").param("quantity", "100")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/exercise/orders/1").param("quantity", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/exercise/orders/1").param("quantity", "99")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Wzorzec 3: błędy ciała klienta jako JSON {\"errors\": {pole: komunikat}}")
    void exercise3() throws Exception {
        mockMvc.perform(post("/exercise/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"to-nie-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty())
                .andExpect(jsonPath("$.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }
}
