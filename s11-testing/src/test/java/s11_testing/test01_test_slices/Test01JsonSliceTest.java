package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test01TestSlices: wycinek {@code @JsonTest} — tylko Jackson (zamiana obiektów na JSON i z
 * powrotem) i pomocnik {@code JacksonTester}. Brak kontrolerów, serwisów, repozytoriów, a nawet serwera. Kiedy
 * przydatny? Gdy DTO ma własne adnotacje {@code @JsonProperty}, formaty dat itp. — sprawdzasz JSON bez całej aplikacji.
 * (W Boot 4 używa Jacksona 3: pakiety {@code tools.jackson}.)
 */
@JsonTest
class Test01JsonSliceTest {

    @Autowired
    JacksonTester<ProductResponse> json;   // tester wstrzykuje sam wycinek

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 2: rekord → JSON: liczba bez cudzysłowu, data Instant jako tekst ISO")
    void serialize() throws Exception {
        ProductResponse response = new ProductResponse(1, "Kawa", new BigDecimal("24.90"), Instant.parse("2026-09-01T10:00:00Z"));

        assertThat(json.write(response)).extractingJsonPathNumberValue("$.price").isEqualTo(24.90);
        assertThat(json.write(response)).extractingJsonPathStringValue("$.createdAt").isEqualTo("2026-09-01T10:00:00Z");
        assertThat(json.write(response)).doesNotHaveJsonPath("$.internalCode");
    }

    @Test
    @DisplayName("Sekcja 2: JSON → rekord")
    void deserialize() throws Exception {
        String content = "{\"id\":2,\"name\":\"Herbata\",\"price\":15.99,\"createdAt\":\"2026-09-02T10:00:00Z\"}";

        assertThat(json.parseObject(content)).isEqualTo(
                new ProductResponse(2, "Herbata", new BigDecimal("15.99"), Instant.parse("2026-09-02T10:00:00Z")));
    }

    @Test
    @DisplayName("Sekcja 2: w wycinku Json nie ma ani kontrolera, ani serwisu, ani repozytorium")
    void sliceContents() {
        assertThat(context.getBeanNamesForType(ProductController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ProductService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(ProductRepository.class)).isEmpty();
    }
}
