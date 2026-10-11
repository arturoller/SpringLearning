package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.accept.NotAcceptableApiVersionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest07ApiVersioning. {@code @WebMvcTest} (bez listy) bierze kontrolery lekcji i ApiVersioningConfig
 * (to WebMvcConfigurer, więc należy do wycinka web). Wersję wysyłamy nagłówkiem API-Version.
 */
@WebMvcTest
class Rest07ApiVersioningTest {

    static final String V = "API-Version";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Ten sam adres, dwie wersje: 1.0 → priceGrosze, 2.0 → obiekt price (zmiana łamiąca)")
    void twoShapesOfOneResource() throws Exception {
        mockMvc.perform(get("/api/products/1").header(V, "1.0"))
                .andExpect(content().json("{\"id\":1,\"name\":\"Kawa\",\"priceGrosze\":2999}", true));
        mockMvc.perform(get("/api/products/1").header(V, "2.0"))
                .andExpect(content().json("{\"id\":1,\"name\":\"Kawa\",\"price\":{\"amount\":29.99,\"currency\":\"PLN\"}}", true));
    }

    @Test
    @DisplayName("2. Wersjonowanie bez wersji domyślnej: brak nagłówka → 400 nawet dla /api/health bez atrybutu version (pytanie 4)")
    void versionRequiredEverywhere() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(MissingApiVersionException.class));
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/health").header(V, "2.0"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    @DisplayName("3. Baseline 1.0+ obejmuje 1.1; \"2\" znaczy to samo co \"2.0\"")
    void baselineAndSemanticParsing() throws Exception {
        mockMvc.perform(get("/api/products/1").header(V, "1.1"))
                .andExpect(content().json("{\"priceGrosze\":2999}"));
        mockMvc.perform(get("/api/products/1").header(V, "2"))
                .andExpect(content().json("{\"price\":{\"currency\":\"PLN\"}}"));
    }

    @Test
    @DisplayName("3. Lista: 1.0 → tablica; 1.1 i 2.0 → strona z total (baseline 1.1+) — pytanie 3")
    void baselineCoversNewerVersions() throws Exception {
        mockMvc.perform(get("/api/products").header(V, "1.0"))
                .andExpect(content().json("[{\"id\":1},{\"id\":2}]"));
        mockMvc.perform(get("/api/products").header(V, "1.1"))
                .andExpect(content().json("{\"total\":2}"));
        mockMvc.perform(get("/api/products").header(V, "2.0"))
                .andExpect(content().json("{\"total\":2}"));
    }

    @Test
    @DisplayName("4. Wersja spoza obsługiwanych (9) i nieczytelna (abc) → 400 InvalidApiVersionException")
    void unsupportedVersion() throws Exception {
        mockMvc.perform(get("/api/products/1").header(V, "9"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(InvalidApiVersionException.class));
        mockMvc.perform(get("/api/products/1").header(V, "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. PUŁAPKA: stałe wersje 1.0 i 2.0 na /price — wersja 1.1 nie ma metody → 400 (pytanie 5)")
    void fixedVersionDoesNotCoverNext() throws Exception {
        mockMvc.perform(get("/api/products/1/price").header(V, "1.0"))
                .andExpect(content().string("2999"));
        mockMvc.perform(get("/api/products/1/price").header(V, "1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(NotAcceptableApiVersionException.class));
    }

    @Test
    @DisplayName("5. Wersja 1.0 jest wycofywana: nagłówki Deprecation, Sunset i Link; wersja 2.0 — bez nich (pytanie 7)")
    void deprecationHeaders() throws Exception {
        mockMvc.perform(get("/api/products/1").header(V, "1.0"))
                .andExpect(header().exists("Deprecation"))
                .andExpect(header().string("Sunset", "Mon, 1 Mar 2027 00:00:00 GMT"))
                .andExpect(header().string("Link", containsString("rel=\"deprecation\"")));
        mockMvc.perform(get("/api/products/1").header(V, "2.0"))
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"));
    }
}
