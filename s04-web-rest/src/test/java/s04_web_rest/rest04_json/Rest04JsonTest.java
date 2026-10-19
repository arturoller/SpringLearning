package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest04Json przez MockMvc — JSON dokładnie taki, jaki dostaje klient (ten sam JsonMapper Boota co
 * w aplikacji). {@code @Import(JsonConfig.class)} — wycinek @WebMvcTest nie ładuje zwykłych klas {@code @Configuration},
 * a chcemy mieć customizer z lekcji.
 */
@WebMvcTest(JsonDemoController.class)
@Import(JsonConfig.class)
class Rest04JsonTest {

    @Autowired
    MockMvc mockMvc;

    private String body(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("2. Rekord bez adnotacji: pola w kolejności rekordu, daty ISO, BigDecimal jako liczba ze skalą")
    void defaultRecordJson() throws Exception {
        assertThat(body("/api/json/card")).isEqualTo("""
                {"sku":"ELE-001","name":"Laptop Pro 14","category":"ELEKTRONIKA","price":5499.99,"stock":7,\
                "addedOn":"2026-10-19","updatedAt":"2026-10-19T08:30:00Z"}""");
    }

    @Test
    @DisplayName("2. PUŁAPKA Jackson 3: enum z własnym toString() trafia do JSON-a jako toString() — „książki”")
    void enumUsesToStringInJackson3() throws Exception {
        assertThat(body("/api/json/labeled")).isEqualTo("""
                {"sku":"KSI-001","category":"książki"}""");
    }

    @Test
    @DisplayName("3. @JsonProperty zmienia nazwę, @JsonIgnore ukrywa pole, @JsonFormat zmienia format daty i liczby")
    void annotations() throws Exception {
        assertThat(body("/api/json/public")).isEqualTo("""
                {"sku":"ELE-001","productName":"Laptop Pro 14","addedOn":"19.10.2026","price":"5499.99"}""");
    }

    @Test
    @DisplayName("4. Pola null: domyślnie \"email\":null; z @JsonInclude(NON_NULL) pola nie ma wcale")
    void nullFields() throws Exception {
        mockMvc.perform(get("/api/json/customer"))
                .andExpect(content().json("""
                        {"id":2,"name":"Maria Nowak","email":null}
                        """, JsonCompareMode.STRICT));

        mockMvc.perform(get("/api/json/customer-without-nulls"))
                .andExpect(content().json("""
                        {"id":2,"name":"Maria Nowak"}
                        """, JsonCompareMode.STRICT))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("5. Nieznane pole w żądaniu jest IGNOROWANE (Jackson 3 i Boot 4: FAIL_ON_UNKNOWN_PROPERTIES = false)")
    void unknownFieldsIgnored() throws Exception {
        mockMvc.perform(post("/api/json/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"SPO-001","name":"Kawa ziarnista 1kg","category":"SPOZYWCZE","price":64.99,"stock":120,
                         "color":"brązowy"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SPO-001"))
                .andExpect(jsonPath("$.color").doesNotExist());
    }

    @Test
    @DisplayName("5. PUŁAPKA Jackson 3: brak pola typu int → 400 (FAIL_ON_NULL_FOR_PRIMITIVES), a nie po cichu 0")
    void missingPrimitiveGives400() throws Exception {
        mockMvc.perform(post("/api/json/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"SPO-001","name":"Kawa ziarnista 1kg","category":"SPOZYWCZE","price":64.99}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpMessageNotReadableException.class)
                        .hasMessageContaining("Cannot map `null` into type `int`"));
    }

    @Test
    @DisplayName("6. Customizer z JsonConfig: enum bez rozróżniania wielkości liter — \"spozywcze\" → SPOZYWCZE")
    void customizerMakesEnumsCaseInsensitive() throws Exception {
        mockMvc.perform(post("/api/json/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"SPO-002","name":"Czekolada gorzka","category":"spozywcze","price":7.49,"stock":300}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SPOZYWCZE"));
    }
}
