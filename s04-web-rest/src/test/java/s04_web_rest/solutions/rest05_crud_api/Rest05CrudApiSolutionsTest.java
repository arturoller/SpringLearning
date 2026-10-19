package s04_web_rest.solutions.rest05_crud_api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprawdzenia z Rest05CrudApiExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
@WebMvcTest(Rest05SolutionController.class)
class Rest05CrudApiSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("Wzorzec 1: lista posortowana po id, bez emaili; jeden klient 200/404")
    void solution1() throws Exception {
        String body = mockMvc.perform(get("/api/cw05/customers"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> ids = JsonPath.read(body, "$[*].id");
        assertThat(ids).startsWith(1, 2, 3).isSorted();
        assertThat(body).doesNotContain("email");

        mockMvc.perform(get("/api/cw05/customers/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Adam Mazur"))
                .andExpect(jsonPath("$.email").doesNotExist());
        mockMvc.perform(get("/api/cw05/customers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Wzorzec 2: POST → 201 + Location + odpowiedź bez emaila")
    void solution2() throws Exception {
        MvcResult result = mockMvc.perform(json(post("/api/cw05/customers"), """
                        {"name":"Piotr Zieliński","city":"Łódź","email":"piotr@example.com","vip":false}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andReturn();
        Integer id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/api/cw05/customers/" + id);
        mockMvc.perform(get("/api/cw05/customers/" + id)).andExpect(jsonPath("$.city").value("Łódź"));
    }

    @Test
    @DisplayName("Wzorzec 3: PUT zastępuje całość, dwa razy = ten sam wynik; nieistniejący → 404")
    void solution3() throws Exception {
        String body = """
                {"name":"Marek Król","city":"Katowice","email":"marek.krol@example.com","vip":true}
                """;
        String first = mockMvc.perform(json(put("/api/cw05/customers/6"), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Katowice"))
                .andExpect(jsonPath("$.vip").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String second = mockMvc.perform(json(put("/api/cw05/customers/6"), body))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(second).isEqualTo(first);

        mockMvc.perform(json(put("/api/cw05/customers/99"), body)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cw05/customers/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Wzorzec 4: PATCH zmienia tylko podane pola; {} nic nie zmienia; nieistniejący → 404")
    void solution4() throws Exception {
        mockMvc.perform(json(patch("/api/cw05/customers/5"), """
                        {"vip":true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vip").value(true))
                .andExpect(jsonPath("$.city").value("Poznań"))
                .andExpect(jsonPath("$.name").value("Ola Pawlak"));

        mockMvc.perform(json(patch("/api/cw05/customers/5"), "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vip").value(true))
                .andExpect(jsonPath("$.city").value("Poznań"));

        mockMvc.perform(json(patch("/api/cw05/customers/99"), "{\"city\":\"Opole\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Wzorzec 5: DELETE 204, potem 404 — i cały cykl życia klienta")
    void solution5() throws Exception {
        MvcResult created = mockMvc.perform(json(post("/api/cw05/customers"), """
                        {"name":"Anna Nowak","city":"Lublin","email":null,"vip":false}
                        """))
                .andExpect(status().isCreated()).andReturn();
        Integer id = JsonPath.read(created.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        mockMvc.perform(get("/api/cw05/customers/" + id)).andExpect(status().isOk());
        mockMvc.perform(json(patch("/api/cw05/customers/" + id), "{\"city\":\"Zamość\"}"))
                .andExpect(jsonPath("$.city").value("Zamość"));
        mockMvc.perform(json(put("/api/cw05/customers/" + id), """
                        {"name":"Anna Nowak-Lis","city":"Zamość","email":"anna@example.com","vip":true}
                        """))
                .andExpect(jsonPath("$.name").value("Anna Nowak-Lis"));
        mockMvc.perform(delete("/api/cw05/customers/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/cw05/customers/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cw05/customers/" + id)).andExpect(status().isNotFound());
    }
}
