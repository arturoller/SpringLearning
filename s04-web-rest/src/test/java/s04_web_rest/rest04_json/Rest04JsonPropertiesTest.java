package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 5 lekcji Rest04Json: ustawienia spring.jackson.* — TYLKO w tym teście ({@code @WebMvcTest(properties = ...)}),
 * więc inne testy i lekcje mają domyślnego Jacksona. Dwa ostatnie testy startują osobny kontekst bez serwera
 * ({@code WebApplicationType.NONE}), żeby porównać inne zestawy ustawień.
 */
@WebMvcTest(controllers = CatalogController.class, properties = {
        "spring.jackson.datatype.datetime.write-dates-as-timestamps=true",
        "spring.jackson.deserialization.fail-on-unknown-properties=true",
        "spring.jackson.default-property-inclusion=non_null"})
class Rest04JsonPropertiesTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    @DisplayName("5. write-dates-as-timestamps=true: LocalDate jako tablica [2026,10,1]")
    void datesAsTimestamps() throws Exception {
        mockMvc.perform(get("/api/catalog/1"))
                .andExpect(jsonPath("$.addedOn[0]").value(2026))
                .andExpect(jsonPath("$.addedOn[1]").value(10))
                .andExpect(jsonPath("$.addedOn[2]").value(1));
    }

    @Test
    @DisplayName("5. fail-on-unknown-properties=true: nieznane pole w zapytaniu → 400")
    void unknownPropertyRejected() throws Exception {
        mockMvc.perform(post("/api/catalog/echo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"product_name\":\"Miód\",\"priceGrosze\":1,\"color\":\"złoty\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. default-property-inclusion=non_null: null pomijany bez żadnej adnotacji")
    void nonNullEverywhere() {
        assertThat(jsonMapper.writeValueAsString(new Promotion("X", null))).isEqualTo("{\"code\":\"X\"}");
    }

    @Test
    @DisplayName("5. use-jackson2-defaults=true: zachowanie jak w Boot 3 (0 zamiast błędu dla int, bez sortowania)")
    void jackson2Defaults() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Rest04Application.class)
                .web(WebApplicationType.NONE)
                .properties("spring.jackson.use-jackson2-defaults=true")
                .run()) {
            JsonMapper mapper = context.getBean(JsonMapper.class);

            assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)).isFalse();
            assertThat(mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)).isFalse();
            assertThat(mapper.readValue("{\"name\":\"Kawa\"}", SecretPriceProduct.class).purchasePriceGrosze()).isZero();
            assertThat(mapper.writeValueAsString(ShelfLabel.of(2999, "Kawa", "590")))
                    .isEqualTo("{\"price\":2999,\"name\":\"Kawa\",\"barcode\":\"590\"}");
        }
    }

    @Test
    @DisplayName("5. PUŁAPKA: klucz z Boot 3 spring.jackson.serialization.write-dates-as-timestamps → aplikacja nie startuje (pytanie 5)")
    void boot3PropertyFailsStartup() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(Rest04Application.class)
                .web(WebApplicationType.NONE)
                .properties("spring.jackson.serialization.write-dates-as-timestamps=false")
                .run()
                .close())
                .rootCause()
                .hasMessageContaining("No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps");
    }
}
