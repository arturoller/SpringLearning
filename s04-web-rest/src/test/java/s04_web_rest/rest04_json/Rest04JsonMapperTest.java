package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.ApplicationContext;
import s04_web_rest.rest04_json.JsonModels.Category;
import s04_web_rest.rest04_json.JsonModels.NewProduct;
import s04_web_rest.rest04_json.JsonModels.ProductCard;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1, 6 i 7 lekcji Rest04Json: {@code @JsonTest} — wycinek kontekstu z SAMYM Jacksonem (JsonMapper Boota +
 * moduły i komponenty Jacksona), bez warstwy webowej. Do tego JacksonTester — pomocnik do porównywania JSON-a.
 * Zwykłe klasy {@code @Configuration} (np. JsonConfig) NIE są tu ładowane, więc customizera z lekcji tu nie ma.
 */
@JsonTest
class Rest04JsonMapperTest {

    @Autowired
    JsonMapper jsonMapper;   // mapper zbudowany przez Boota (JacksonAutoConfiguration)

    @Autowired
    ObjectMapper objectMapper;   // ten sam obiekt — JsonMapper dziedziczy po ObjectMapper

    @Autowired
    JacksonTester<ProductCard> cardJson;   // tester = sprawdzacz JSON-a dla typu ProductCard

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Boot 4 daje bean JsonMapper z Jacksona 3 (pakiet tools.jackson); ObjectMapper to ten sam obiekt")
    void bootProvidesJackson3JsonMapper() {
        assertThat(jsonMapper.getClass().getName()).isEqualTo("tools.jackson.databind.json.JsonMapper");
        assertThat(objectMapper).isSameAs(jsonMapper);
        assertThat(context.getBeanNamesForType(JsonMapper.class)).containsExactly("jacksonJsonMapper");
    }

    @Test
    @DisplayName("1. Wyjątki Jacksona 3 są NIEKONTROLOWANE (RuntimeException) — bez throws i try/catch")
    void jackson3ExceptionsAreUnchecked() {
        assertThat(RuntimeException.class).isAssignableFrom(JacksonException.class);

        assertThatThrownBy(() -> jsonMapper.readValue("{\"stock\":\"dużo\"}", NewProduct.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageStartingWith("Cannot deserialize value of type `int` from String \"dużo\"");
    }

    @Test
    @DisplayName("2. JacksonTester: zapis rekordu do JSON-a i odczyt z powrotem (daty i BigDecimal bez strat)")
    void jacksonTesterRoundTrip() throws Exception {
        ProductCard card = new ProductCard("KSI-001", "Czysty kod", Category.KSIAZKI, new BigDecimal("79.00"), 15,
                LocalDate.of(2026, 10, 19), Instant.parse("2026-10-19T08:30:00Z"));

        assertThat(cardJson.write(card))
                .extractingJsonPathStringValue("$.addedOn").isEqualTo("2026-10-19");
        assertThat(cardJson.write(card))
                .extractingJsonPathNumberValue("$.price").isEqualTo(79.00);

        assertThat(cardJson.parseObject(cardJson.write(card).getJson())).isEqualTo(card);
    }

    @Test
    @DisplayName("2. PUŁAPKA Jackson 3: zwykła klasa (nie rekord) ma pola w kolejności ALFABETYCZNEJ")
    void plainClassPropertiesAreSortedAlphabetically() {
        assertThat(jsonMapper.writeValueAsString(new LegacyProduct())).isEqualTo("""
                {"name":"Czysty kod","price":79.00,"sku":"KSI-001"}""");
    }

    @Test
    @DisplayName("6. Bez customizera z JsonConfig (tu go nie ma) enum rozróżnia wielkość liter — \"ksiazki\" to błąd")
    void withoutCustomizerEnumIsCaseSensitive() {
        assertThatThrownBy(() -> jsonMapper.readValue("""
                {"sku":"KSI-001","name":"Czysty kod","category":"ksiazki","price":79.00,"stock":15}
                """, NewProduct.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("not one of the values accepted for Enum class");
    }

    @Test
    @DisplayName("7. Lista z JSON-a: TypeReference z Jacksona 3 (tools.jackson.core.type.TypeReference)")
    void readListWithTypeReference() {
        List<NewProduct> products = jsonMapper.readValue("""
                [{"sku":"SPO-001","name":"Kawa ziarnista 1kg","category":"SPOZYWCZE","price":64.99,"stock":120},
                 {"sku":"SPO-002","name":"Czekolada gorzka","category":"SPOZYWCZE","price":7.49,"stock":300}]
                """, new TypeReference<List<NewProduct>>() {
        });

        assertThat(products).extracting(NewProduct::sku).containsExactly("SPO-001", "SPO-002");
    }

    /** Zwykła klasa z getterami (jak w starszym kodzie) — pola zadeklarowane w kolejności sku, price, name. */
    static class LegacyProduct {

        public String getSku() {
            return "KSI-001";
        }

        public BigDecimal getPrice() {
            return new BigDecimal("79.00");
        }

        public String getName() {
            return "Czysty kod";
        }
    }
}
