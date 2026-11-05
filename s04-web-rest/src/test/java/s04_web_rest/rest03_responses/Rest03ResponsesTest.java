package s04_web_rest.rest03_responses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest03Responses. Wycinek: {@code @WebMvcTest} + MockMvc. Magazyn produktów (zwykły @Component)
 * dodajemy przez {@code @Import}, a przed każdym testem przywracamy jego stan początkowy — testy nie zależą od kolejności.
 */
@WebMvcTest
@Import(ProductStore.class)
class Rest03ResponsesTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductStore store;

    @BeforeEach
    void resetStore() {
        store.reset();
    }

    @Test
    @DisplayName("1. ResponseEntity.ok → 200 z ciałem, notFound → 404 bez ciała")
    void okAndNotFound() throws Exception {
        mockMvc.perform(get("/rest03/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"));

        mockMvc.perform(get("/rest03/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("1. ResponseEntity.created → 201, nagłówek Location z adresem nowego zasobu i ciało")
    void created() throws Exception {
        mockMvc.perform(post("/rest03/products").contentType("application/json")
                        .content("{\"name\":\"Ciastka\",\"priceGrosze\":1299}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/rest03/products/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Ciastka"));

        // pod adresem z Location nowy produkt naprawdę jest
        mockMvc.perform(get("/rest03/products/3")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("1. badRequest → 400 (brak nazwy), status(CONFLICT) → 409 (duplikat nazwy)")
    void badRequestAndConflict() throws Exception {
        mockMvc.perform(post("/rest03/products").contentType("application/json")
                        .content("{\"priceGrosze\":100}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/rest03/products").contentType("application/json")
                        .content("{\"name\":\"kawa\",\"priceGrosze\":100}"))   // „kawa” ≈ istniejąca „Kawa”
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("1. noContent → 204 po usunięciu (ciało puste); drugie usunięcie tego samego → 404")
    void noContent() throws Exception {
        mockMvc.perform(delete("/rest03/products/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/rest03/products/1")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("2. @ResponseStatus na metodzie → 201, ale bez nagłówka Location")
    void responseStatusOnMethod() throws Exception {
        mockMvc.perform(post("/rest03/products/quick").contentType("application/json")
                        .content("{\"name\":\"Ciastka\",\"priceGrosze\":1299}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.name").value("Ciastka"));
    }

    @Test
    @DisplayName("2. PUŁAPKA: @ResponseStatus(CREATED) przy zwracanym ResponseEntity.ok jest ignorowany — odpowiedź to 200")
    void responseEntityWinsOverResponseStatus() throws Exception {
        mockMvc.perform(get("/rest03/products/1/both")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. @ResponseStatus na klasie wyjątku → rzucenie SoldOutException daje 409")
    void responseStatusOnException() throws Exception {
        mockMvc.perform(post("/rest03/products/1/reserve")).andExpect(status().isOk());

        mockMvc.perform(post("/rest03/products/2/reserve"))
                .andExpect(status().isConflict())
                .andExpect(status().reason("Towar wyprzedany"))
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(SoldOutException.class));
    }

    @Test
    @DisplayName("3. ResponseStatusException → kod i powód podane w miejscu rzucenia")
    void responseStatusException() throws Exception {
        mockMvc.perform(get("/rest03/products/1/or-fail")).andExpect(status().isOk());

        mockMvc.perform(get("/rest03/products/99/or-fail"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Produkt 99 nie istnieje"));
    }

    @Test
    @DisplayName("4. Nagłówki odpowiedzi: własny X-Total-Count oraz Cache-Control i ETag")
    void responseHeaders() throws Exception {
        mockMvc.perform(get("/rest03/products"))
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/rest03/products/1/cached"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(header().string("ETag", "\"v1-1\""));
    }

    @Test
    @DisplayName("5. PUŁAPKA: null z metody to 200 z pustym ciałem, a NIE 404")
    void nullIsNot404() throws Exception {
        mockMvc.perform(get("/rest03/products/99/maybe"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        // porównaj z poprawną wersją tej samej sytuacji: ResponseEntity.notFound()
        mockMvc.perform(get("/rest03/products/99")).andExpect(status().isNotFound());
    }
}
