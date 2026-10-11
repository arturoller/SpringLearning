package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sekcja 3: wszystko razem — prawdziwy serwis, kontroler i advice (cały kontekst + MockMvc). */
@SpringBootTest
@AutoConfigureMockMvc
class Err05CustomExceptionsEndToEndTest {

    @Autowired
    MockMvc mockMvc;

    private ResultActions order(String customer, long productId, int quantity) throws Exception {
        String json = "{\"customer\":\"" + customer + "\",\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
        return mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("3. Całość: 201 dla poprawnego zamówienia, 404/409/502/500 dla błędów — każdy jako problem+json")
    void wholeFlow() throws Exception {
        order("Ala", 1, 2).andExpect(status().isCreated()).andExpect(jsonPath("$.productId").value(1));
        order("Ala", 999, 1).andExpect(status().isNotFound());
        order("Ala", 2, 1).andExpect(status().isConflict()).andExpect(jsonPath("$.available").value(0));
        order("gateway-down", 1, 1).andExpect(status().isBadGateway());
        order("boom", 1, 1).andExpect(status().isInternalServerError());
    }
}
