package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lekcja Rest04Json, sekcja 4: ten sam kontroler, ale z włączonym ustawieniem
 * {@code spring.jackson.deserialization.fail-on-unknown-properties=true} — nieznane pole kończy się błędem 400.
 */
@WebMvcTest(value = JsonController.class,
        properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
class Rest04UnknownFieldsStrictTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("4. Z fail-on-unknown-properties=true: nieznane pole → 400, poprawny JSON → 200")
    void unknownFieldRejected() throws Exception {
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json")
                        .content("{\"id\":1,\"name\":\"Kawa\",\"price\":24.90,\"color\":\"brązowy\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/rest04/products/echo").contentType("application/json")
                        .content("{\"id\":1,\"name\":\"Kawa\",\"price\":24.90}"))
                .andExpect(status().isOk());
    }
}
