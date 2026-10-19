package s04_web_rest.rest03_responses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Rest03Responses. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Nierozwiązane ćwiczenie pada z UnsupportedOperationException: TODO.
 */
@Tag("cwiczenie")
@WebMvcTest(Rest03ExerciseController.class)
class Rest03ResponsesExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: GET klienta → 200 i JSON; nieznany id → 404 z pustym ciałem")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/cw03/customers/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan Kowalski"))
                .andExpect(jsonPath("$.vip").value(true));
        mockMvc.perform(get("/api/cw03/customers/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Ćwiczenie 2: POST → 201, Location z adresem nowego klienta i klient w ciele")
    void exercise2() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cw03/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Piotr Zieliński","city":"Łódź","email":"piotr@example.com","vip":false}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Piotr Zieliński"))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        assertThat(id).isGreaterThanOrEqualTo(8);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/api/cw03/customers/" + id);
    }

    @Test
    @DisplayName("Ćwiczenie 3: DELETE → 204 bez ciała; drugi DELETE tego samego klienta → 404")
    void exercise3() throws Exception {
        mockMvc.perform(delete("/api/cw03/customers/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete("/api/cw03/customers/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ćwiczenie 4: ten sam adres — Accept: text/plain → wizytówka, JSON → JSON, XML → 406")
    void exercise4() throws Exception {
        String vip = mockMvc.perform(get("/api/cw03/customers/4").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(vip).isEqualTo("Zofia Krawczyk, Warszawa (VIP)");

        String regular = mockMvc.perform(get("/api/cw03/customers/2").accept(MediaType.TEXT_PLAIN))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(regular).isEqualTo("Maria Nowak, Kraków");

        mockMvc.perform(get("/api/cw03/customers/99").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cw03/customers/3").accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("Gdańsk"));
        mockMvc.perform(get("/api/cw03/customers/3").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
}
