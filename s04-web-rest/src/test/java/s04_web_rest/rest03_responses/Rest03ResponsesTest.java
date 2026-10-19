package s04_web_rest.rest03_responses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest03Responses. {@code @WebMvcTest} z kontrolerami lekcji (bez szkieletu ćwiczeń).
 * Kontroler trzyma produkty w pamięci, a kontekst testu jest wspólny dla wszystkich metod — dlatego każdy test,
 * który coś dodaje lub usuwa, używa WŁASNEGO sku i nie zależy od kolejności testów.
 */
@WebMvcTest(controllers = {ProductController.class, NullPitfallController.class})
class Rest03ResponsesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. ResponseEntity: 200, własny nagłówek X-Total-Count i lista w ciele")
    void responseEntityWithHeader() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$[0].sku").value("ELE-001"));
    }

    @Test
    @DisplayName("1. ResponseEntity.of(Optional): jest → 200 i JSON, brak → 404 z pustym ciałem")
    void responseEntityOfOptional() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro 14"));

        mockMvc.perform(get("/api/products/XXX-999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("2. POST → 201 Created, Location z pełnym adresem nowego produktu i produkt w ciele (pytanie 2)")
    void createReturns201WithLocation() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"ODZ-002","name":"T-shirt bawełniany","price":49.99,"stock":80}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/ODZ-002"))
                .andExpect(jsonPath("$.sku").value("ODZ-002"));

        // Location prowadzi do nowego zasobu
        mockMvc.perform(get("http://localhost/api/products/ODZ-002").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. Drugi POST z tym samym sku → 409 Conflict z ResponseStatusException (pytanie 3)")
    void secondCreateGives409() throws Exception {
        String body = """
                {"sku":"ODZ-001","name":"Kurtka zimowa","price":459.00,"stock":12}
                """;
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ResponseStatusException.class));
    }

    @Test
    @DisplayName("2. @ResponseStatus(CREATED) na metodzie → 201, ale BEZ nagłówka Location")
    void responseStatusOnMethod() throws Exception {
        mockMvc.perform(post("/api/products/quick").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"DOM-001","name":"Ekspres do kawy","price":1899.00,"stock":2}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @DisplayName("3. DELETE → 204 No Content bez ciała; drugi DELETE tego samego → 404 (pytanie 8)")
    void deleteGives204Then404() throws Exception {
        mockMvc.perform(post("/api/products/quick").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"DOM-002","name":"Lampka biurkowa","price":129.00,"stock":18}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/products/DOM-002"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/api/products/DOM-002"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. ResponseStatusException(NOT_FOUND, powód) → 404, powód trafia do komunikatu błędu")
    void responseStatusException() throws Exception {
        mockMvc.perform(get("/api/products/XXX-999/details"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResponse().getErrorMessage()).isEqualTo("Nie ma produktu XXX-999"));
    }

    @Test
    @DisplayName("4. PUŁAPKA: wyjątek z @ResponseStatus(reason) → 404 ze STAŁYM powodem z adnotacji, nie z wyjątku")
    void reasonFromAnnotationWins() throws Exception {
        mockMvc.perform(get("/api/products/XXX-999/stock"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).hasMessage("Nie ma produktu XXX-999");
                    assertThat(result.getResponse().getErrorMessage()).isEqualTo("Nie ma takiego produktu");
                });
    }

    @Test
    @DisplayName("5. Ten sam adres, różne produces: Accept wybiera JSON albo tekst; bez Accept wygrywa JSON")
    void contentNegotiation() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        String text = mockMvc.perform(get("/api/products/ELE-001").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(text).isEqualTo("Laptop Pro 14 — 5499.99 zł");

        mockMvc.perform(get("/api/products/ELE-001"))   // brak Accept: remis rozstrzygany alfabetycznie
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("5. Accept: application/xml → 406 Not Acceptable (HttpMediaTypeNotAcceptableException)")
    void notAcceptable() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpMediaTypeNotAcceptableException.class)
                        .hasMessage("No acceptable representation"));
    }

    @Test
    @DisplayName("5. consumes JSON, a Content-Type: text/plain → 415 i nagłówek Accept z przyjmowanym typem")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", "application/json"))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpMediaTypeNotSupportedException.class)
                        .hasMessage("Content-Type 'text/plain' is not supported"));
    }

    @Test
    @DisplayName("6. PUŁAPKA: return null → 200 OK i puste ciało, zarówno dla Product, jak i ResponseEntity (pytanie 4)")
    void nullReturnGives200WithEmptyBody() throws Exception {
        mockMvc.perform(get("/api/pitfalls/null-product/XXX-999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/pitfalls/null-entity/XXX-999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
