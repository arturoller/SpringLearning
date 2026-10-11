package s04_web_rest.rest03_responses;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest03Responses. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ResponseExerciseController.class)
class Rest03ResponsesExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: znany kupon → 200 z ciałem, nieznany → 404 bez ciała")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/exercise/coupons/LATO10"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":\"LATO10\",\"percent\":10}"));
        mockMvc.perform(get("/api/exercise/coupons/NIEMA"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Ćwiczenie 2: POST opinii → 201 + Location; złe gwiazdki → 400")
    void exercise2() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exercise/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"stars\":5,\"text\":\"Pyszna kawa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stars").value(5))
                .andReturn();
        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/api/exercise/reviews/" + id);

        mockMvc.perform(post("/api/exercise/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"stars\":6,\"text\":\"Za dużo gwiazdek\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Ćwiczenie 3: PUT stanu — nowy → 201 + Location, istniejący → 200, ujemny → 400")
    void exercise3() throws Exception {
        mockMvc.perform(put("/api/exercise/stock/7").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/exercise/stock/7"))
                .andExpect(content().json("{\"productId\":7,\"quantity\":12}"));
        mockMvc.perform(put("/api/exercise/stock/7").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"productId\":7,\"quantity\":12}"));
        mockMvc.perform(put("/api/exercise/stock/7").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/exercise/stock/7"))
                .andExpect(content().json("{\"productId\":7,\"quantity\":12}"));
    }
}
