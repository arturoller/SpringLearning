package s04_web_rest.rest03_responses;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest03Responses. {@code @WebMvcTest} nie tworzy zwykłych komponentów ({@code @Component}), więc
 * prawdziwy ProductStore dokładamy przez {@code @Import} — jest prosty i w pamięci, atrapa nie jest potrzebna.
 * Testy, które coś dodają lub usuwają, działają na WŁASNYCH produktach — nie zależą od kolejności uruchamiania.
 */
@WebMvcTest(ProductResponseController.class)
@Import(ProductStore.class)
class Rest03ResponsesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Zwykły obiekt → 200 i JSON")
    void plainObjectIs200() throws Exception {
        mockMvc.perform(get("/api/products/plain/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"));
    }

    @Test
    @DisplayName("1. PUŁAPKA: null z kontrolera → 200 i PUSTE ciało, a nie 404 (pytanie 2)")
    void nullIs200WithEmptyBody() throws Exception {
        mockMvc.perform(get("/api/products/plain/99"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("2. ResponseEntity.of(Optional): jest → 200 z ciałem, brak → 404 bez ciała")
    void responseEntityOfOptional() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"));
        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("2. Wersja „ręczna” (map + orElseGet): nagłówek tylko przy 200")
    void manualResponseEntity() throws Exception {
        mockMvc.perform(get("/api/products/manual/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Product-Name-Length", "4"));
        mockMvc.perform(get("/api/products/manual/99"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("X-Product-Name-Length"));
    }

    @Test
    @DisplayName("3. POST → 201 Created, Location z pełnym adresem nowego produktu i produkt w ciele")
    void createdWithLocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciastka\",\"priceGrosze\":1299}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ciastka"))
                .andReturn();

        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("http://localhost/api/products/" + id);
        mockMvc.perform(get("/api/products/" + id)).andExpect(status().isOk());   // Location prowadzi do produktu
    }

    @Test
    @DisplayName("3. DELETE → 204 No Content bez ciała; drugi raz → 404")
    void deleteNoContent() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Do usunięcia\",\"priceGrosze\":100}"))
                .andReturn();
        long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. @ResponseStatus na metodzie → 201, ale bez nagłówka Location")
    void responseStatusOnMethod() throws Exception {
        mockMvc.perform(post("/api/products/quick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Woda\",\"priceGrosze\":199}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @DisplayName("4. @ResponseStatus na klasie wyjątku → rzucony ProductNotFoundException daje 404")
    void responseStatusOnException() throws Exception {
        mockMvc.perform(get("/api/products/99/details"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ProductNotFoundException.class));
        mockMvc.perform(get("/api/products/1/details")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. PUŁAPKA: @ResponseStatus(CREATED) + ResponseEntity.ok(...) → wygrywa ResponseEntity: 200 (pytanie 5)")
    void responseEntityWinsOverResponseStatus() throws Exception {
        mockMvc.perform(get("/api/products/conflict"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. Własne nagłówki: X-Total-Count i Cache-Control: max-age=60; 202 Accepted z treścią")
    void customHeadersAndAccepted() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().string("Cache-Control", "max-age=60"));
        mockMvc.perform(post("/api/products/import"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Import przyjęty do realizacji"));
    }
}
