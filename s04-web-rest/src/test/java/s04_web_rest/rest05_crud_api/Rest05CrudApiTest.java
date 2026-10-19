package s04_web_rest.rest05_crud_api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest05CrudApi. {@code @WebMvcTest} ładuje tylko kontroler; repozytorium (zwykły bean, nie kontroler)
 * dokładamy przez {@code @Import} — prawdziwe, nie atrapa, bo to ono trzyma stan, który sprawdzamy.
 * Kontekst (i repozytorium) jest wspólny dla wszystkich testów tej klasy, dlatego każdy test, który coś zmienia,
 * pracuje na WŁASNYM sku albo przywraca stan — testy nie zależą od kolejności.
 */
@WebMvcTest(ProductCrudController.class)
@Import(ProductRepository.class)
class Rest05CrudApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductRepository repository;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("1. Odpowiedź to DTO: bez purchasePrice (tajemnica sklepu), z wyliczonym polem available")
    void responseIsDtoNotModel() throws Exception {
        mockMvc.perform(get("/api/products/ELE-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("ELE-002"))
                .andExpect(jsonPath("$.available").value(false))      // stock = 0
                .andExpect(jsonPath("$.purchasePrice").doesNotExist());
    }

    @Test
    @DisplayName("2. GET listy → 200 i produkty posortowane po sku (mapa nie ma kolejności — sortuje repozytorium)")
    void listIsSorted() throws Exception {
        String body = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> skus = JsonPath.read(body, "$[*].sku");   // JsonPath — czytanie fragmentów JSON-a ścieżką
        assertThat(skus).contains("ELE-001", "ELE-002", "KSI-001").isSorted();
    }

    @Test
    @DisplayName("3. Cały cykl życia: POST 201 → GET 200 → PUT 200 → PATCH 200 → DELETE 204 → GET 404")
    void fullLifecycle() throws Exception {
        mockMvc.perform(json(post("/api/products"), """
                        {"sku":"DOM-001","name":"Ekspres do kawy","category":"DOM","price":1899.00,
                         "purchasePrice":1400.00,"stock":2}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/DOM-001"));

        mockMvc.perform(get("/api/products/DOM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ekspres do kawy"));

        mockMvc.perform(json(put("/api/products/DOM-001"), """
                        {"name":"Ekspres do kawy Deluxe","category":"DOM","price":2199.00,"purchasePrice":1600.00,"stock":3}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ekspres do kawy Deluxe"))
                .andExpect(jsonPath("$.price").value(2199.00));

        mockMvc.perform(json(patch("/api/products/DOM-001"), """
                        {"stock":0}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.name").value("Ekspres do kawy Deluxe"));   // reszta bez zmian

        mockMvc.perform(delete("/api/products/DOM-001"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/DOM-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. POST nie jest idempotentny: drugi raz ten sam produkt → 409 Conflict (pytanie 2)")
    void postTwiceGives409() throws Exception {
        String body = """
                {"sku":"ODZ-001","name":"Kurtka zimowa","category":"ODZIEZ","price":459.00,"purchasePrice":300.00,"stock":12}
                """;
        mockMvc.perform(json(post("/api/products"), body)).andExpect(status().isCreated());
        mockMvc.perform(json(post("/api/products"), body)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("4. PUT jest idempotentny: dwa takie same PUT → ten sam stan i ta sama odpowiedź")
    void putIsIdempotent() throws Exception {
        String body = """
                {"name":"Kawa ziarnista 1kg","category":"SPOZYWCZE","price":69.99,"purchasePrice":41.00,"stock":120}
                """;
        String first = mockMvc.perform(json(put("/api/products/SPO-001"), body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Product stateAfterFirst = repository.findBySku("SPO-001").orElseThrow();

        String second = mockMvc.perform(json(put("/api/products/SPO-001"), body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(repository.findBySku("SPO-001")).contains(stateAfterFirst);
    }

    @Test
    @DisplayName("4. DELETE jest idempotentny: 204, potem 404 — ale STAN po obu wywołaniach ten sam (pytanie 3)")
    void deleteIsIdempotentButStatusDiffers() throws Exception {
        repository.save(new Product("DOM-002", "Lampka biurkowa", Product.Category.DOM, new BigDecimal("129.00"),
                new BigDecimal("80.00"), 18));

        mockMvc.perform(delete("/api/products/DOM-002")).andExpect(status().isNoContent());
        assertThat(repository.existsBySku("DOM-002")).isFalse();

        mockMvc.perform(delete("/api/products/DOM-002")).andExpect(status().isNotFound());
        assertThat(repository.existsBySku("DOM-002")).isFalse();
    }

    @Test
    @DisplayName("5. PATCH zmienia tylko podane pola; pusty PATCH {} niczego nie zmienia")
    void patchChangesOnlyGivenFields() throws Exception {
        repository.save(new Product("KSI-002", "Java. Podstawy", Product.Category.KSIAZKI, new BigDecimal("129.00"),
                new BigDecimal("90.00"), 9));

        mockMvc.perform(json(patch("/api/products/KSI-002"), """
                        {"price":119.00}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(119.00))
                .andExpect(jsonPath("$.stock").value(9))
                .andExpect(jsonPath("$.name").value("Java. Podstawy"));

        Product before = repository.findBySku("KSI-002").orElseThrow();
        mockMvc.perform(json(patch("/api/products/KSI-002"), "{}")).andExpect(status().isOk());
        assertThat(repository.findBySku("KSI-002")).contains(before);
    }

    @Test
    @DisplayName("6. DTO chroni przed nadpisaniem pól: purchasePrice w PATCH i sku w PUT są po cichu pomijane (pytanie 5)")
    void dtoProtectsFields() throws Exception {
        repository.save(new Product("KSI-003", "Wzorce projektowe", Product.Category.KSIAZKI, new BigDecimal("99.00"),
                new BigDecimal("60.00"), 3));

        mockMvc.perform(json(patch("/api/products/KSI-003"), """
                        {"purchasePrice":1.00}
                        """))
                .andExpect(status().isOk());
        assertThat(repository.findBySku("KSI-003").orElseThrow().purchasePrice()).isEqualByComparingTo("60.00");

        mockMvc.perform(json(put("/api/products/KSI-003"), """
                        {"sku":"XXX-999","name":"Wzorce projektowe","category":"KSIAZKI","price":99.00,
                         "purchasePrice":60.00,"stock":3}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("KSI-003"));   // sku z adresu, nie z ciała
        assertThat(repository.existsBySku("XXX-999")).isFalse();
    }

    @Test
    @DisplayName("6. PUT to zastąpienie CAŁOŚCI: brak pola stock → 400 (PUT ≠ PATCH)")
    void putRequiresAllFields() throws Exception {
        mockMvc.perform(json(put("/api/products/ELE-001"), """
                        {"name":"Laptop Pro 14","category":"ELEKTRONIKA","price":5499.99,"purchasePrice":4300.00}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2. PUT, PATCH i DELETE nieistniejącego produktu → 404; PUT NIE tworzy nowego produktu")
    void missingProductGives404() throws Exception {
        mockMvc.perform(json(put("/api/products/XXX-404"), """
                        {"name":"x","category":"DOM","price":1.00,"purchasePrice":1.00,"stock":1}
                        """))
                .andExpect(status().isNotFound());
        mockMvc.perform(json(patch("/api/products/XXX-404"), "{\"stock\":1}")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/products/XXX-404")).andExpect(status().isNotFound());

        assertThat(repository.existsBySku("XXX-404")).isFalse();
    }
}
