package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest04Json. {@code @WebMvcTest} tworzy też bean JsonMapper Boota (autokonfiguracja Jacksona należy do
 * wycinka web), więc możemy go wstrzyknąć i sprawdzić jego ustawienia wprost.
 */
@WebMvcTest(CatalogController.class)
class Rest04JsonTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;   // bean Boota: tools.jackson.databind.json.JsonMapper

    @Autowired
    ObjectMapper objectMapper;   // nadklasa — ten sam obiekt

    @Test
    @DisplayName("1. Boot 4 tworzy bean JsonMapper (Jackson 3); jako ObjectMapper to ten sam obiekt; wyjątki niesprawdzane")
    void jsonMapperBean() {
        assertThat(objectMapper).isSameAs(jsonMapper);
        assertThat(JsonMapper.class.getPackageName()).isEqualTo("tools.jackson.databind.json");
        assertThat(com.fasterxml.jackson.annotation.JsonProperty.class.getPackageName())
                .isEqualTo("com.fasterxml.jackson.annotation");
        assertThat(RuntimeException.class).isAssignableFrom(JacksonException.class);
        // writeValueAsString bez try/catch — kompiluje się, bo JacksonException jest niesprawdzany
        assertThat(jsonMapper.writeValueAsString(new Promotion("X", null))).isEqualTo("{\"code\":\"X\",\"validUntil\":null}");
    }

    @Test
    @DisplayName("2. Rekord z adnotacjami: product_name, bez purchasePriceGrosze, daty jako tekst")
    void recordWithAnnotations() throws Exception {
        mockMvc.perform(get("/api/catalog/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":1,"product_name":"Kawa","priceGrosze":2999,"description":"Ziarnista, 1 kg",
                         "addedOn":"2026-10-01","updatedAt":"2026-10-01T08:30:00Z"}""", true))   // true = dokładnie te pola
                .andExpect(jsonPath("$.purchasePriceGrosze").doesNotExist());
    }

    @Test
    @DisplayName("2. @JsonInclude(NON_NULL): opis równy null w ogóle nie trafia do JSON-a")
    void nonNullInclusion() throws Exception {
        mockMvc.perform(get("/api/catalog/2"))
                .andExpect(jsonPath("$.product_name").value("Herbata"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("2. Odczyt rekordu: @JsonProperty działa w obie strony, @JsonIgnore na Integer → null")
    void readRecord() throws Exception {
        mockMvc.perform(post("/api/catalog/echo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":5,"product_name":"Miód","priceGrosze":2499,"addedOn":"2026-10-05",
                         "updatedAt":"2026-10-05T10:00:00Z","purchasePriceGrosze":1500}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_name").value("Miód"))
                .andExpect(jsonPath("$.purchasePriceGrosze").doesNotExist());
    }

    @Test
    @DisplayName("2. PUŁAPKA: @JsonIgnore na int w rekordzie → każdy odczyt kończy się 400 (pytanie 4)")
    void jsonIgnoreOnPrimitiveBreaksReading() throws Exception {
        mockMvc.perform(post("/api/catalog/secret-echo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kawa\",\"purchasePriceGrosze\":1800}"))
                .andExpect(status().isBadRequest());
        assertThatThrownBy(() -> jsonMapper.readValue("{\"name\":\"Kawa\"}", SecretPriceProduct.class))
                .isInstanceOf(MismatchedInputException.class)
                .hasMessageContaining("Cannot map `null` into type `int`");
        // zapis działa — pole znika
        assertThat(jsonMapper.writeValueAsString(new SecretPriceProduct("Kawa", 1800))).isEqualTo("{\"name\":\"Kawa\"}");
    }

    @Test
    @DisplayName("3. java.time jako tekst ISO-8601 — także w „gołym” JsonMapper.builder() (domyślne Jacksona 3)")
    void datesAsIsoStrings() {
        record Times(LocalDate date, LocalDateTime dateTime, Instant instant, Duration duration) {
        }
        Times times = new Times(LocalDate.of(2026, 10, 1), LocalDateTime.of(2026, 10, 1, 8, 30),
                Instant.parse("2026-10-01T08:30:00Z"), Duration.ofMinutes(90));
        String expected = "{\"date\":\"2026-10-01\",\"dateTime\":\"2026-10-01T08:30:00\","
                + "\"instant\":\"2026-10-01T08:30:00Z\",\"duration\":\"PT1H30M\"}";

        assertThat(jsonMapper.writeValueAsString(times)).isEqualTo(expected);
        assertThat(JsonMapper.builder().build().writeValueAsString(times)).isEqualTo(expected);
        assertThat(JsonMapper.builder().build().isEnabled(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    @DisplayName("3. @JsonFormat(pattern = dd.MM.yyyy) dla jednego pola; zły format daty w zapytaniu → 400")
    void jsonFormat() throws Exception {
        mockMvc.perform(get("/api/catalog/promotion"))
                .andExpect(content().json("{\"code\":\"JESIEN\",\"validUntil\":\"31.10.2026\"}"));
        assertThat(jsonMapper.readValue("{\"code\":\"A\",\"validUntil\":\"05.11.2026\"}", Promotion.class).validUntil())
                .isEqualTo(LocalDate.of(2026, 11, 5));
        mockMvc.perform(post("/api/catalog/echo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"product_name\":\"Miód\",\"priceGrosze\":1,\"addedOn\":\"01.10.2026\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Domyślne Jacksona 3 w Boot 4: nieznane pola ignorowane, null dla int → błąd, sortowanie alfabetyczne")
    void jackson3Defaults() throws Exception {
        assertThat(jsonMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        assertThat(jsonMapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)).isTrue();
        assertThat(jsonMapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)).isTrue();

        // nieznane pole "color" → 200, pole zignorowane (pytanie 2)
        mockMvc.perform(post("/api/catalog/echo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"product_name\":\"Miód\",\"priceGrosze\":1,\"color\":\"złoty\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").doesNotExist());
        // brak pola priceGrosze (int) → 400 (pytanie 3)
        mockMvc.perform(post("/api/catalog/echo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":5,\"product_name\":\"Miód\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Kolejność: zwykła klasa bez konstruktora z parametrami — alfabetycznie; rekord — kolejność składników")
    void propertyOrder() {
        assertThat(jsonMapper.writeValueAsString(ShelfLabel.of(2999, "Kawa", "5901234123457")))
                .isEqualTo("{\"barcode\":\"5901234123457\",\"name\":\"Kawa\",\"price\":2999}");
        assertThat(jsonMapper.writeValueAsString(new Promotion("JESIEN", LocalDate.of(2026, 10, 31))))
                .isEqualTo("{\"code\":\"JESIEN\",\"validUntil\":\"31.10.2026\"}");
    }
}
