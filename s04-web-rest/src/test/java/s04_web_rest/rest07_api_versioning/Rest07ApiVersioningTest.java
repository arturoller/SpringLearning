package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.client.ApiVersionInserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest07ApiVersioning — strategia „wersja w nagłówku API-Version”, wersja domyślna 1.0.
 * Wycinek: {@code @WebMvcTest} + profil {@code rest07} (czyli application-rest07.properties). Bez profilu ten kontekst
 * by nie wystartował: mapowania z atrybutem version wymagają skonfigurowanej strategii (Rest07JavaConfigTest).
 */
@WebMvcTest
@ActiveProfiles(Rest07Application.PROFILE)
@Import(ProductCatalog.class)
class Rest07ApiVersioningTest {

    private static final String VERSION_HEADER = "API-Version";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("2. Ten sam adres, nagłówek API-Version: 1.0 → cena w groszach, 2.0 → cena jako obiekt {amount, currency}")
    void versionFromHeader() throws Exception {
        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceGrosze").value(2490))
                .andExpect(jsonPath("$.price").doesNotExist());

        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.amount").value(24.90))
                .andExpect(jsonPath("$.price.currency").value("PLN"))
                .andExpect(jsonPath("$.priceGrosze").doesNotExist());
    }

    @Test
    @DisplayName("2. MockMvc (Spring 7): apiVersion(\"2.0\") + ApiVersionInserter wstawia wersję tam, gdzie każe strategia")
    void mockMvcApiVersion() throws Exception {
        mockMvc.perform(get("/rest07/products/1")
                        .apiVersionInserter(ApiVersionInserter.useHeader(VERSION_HEADER))   // inserter = „wstawiacz” wersji
                        .apiVersion("2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.currency").value("PLN"));
    }

    @Test
    @DisplayName("2. Wersje są semantyczne: „1”, „v1” i „1.0.0” to ta sama wersja co „1.0”")
    void semanticVersions() throws Exception {
        for (String version : new String[] {"1", "v1", "1.0.0"}) {
            mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, version))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.priceGrosze").value(2490));
        }
    }

    @Test
    @DisplayName("4. Bez nagłówka → wersja domyślna (1.0); mapowanie bez atrybutu version działa w każdej wersji")
    void defaultVersionAndUnversionedMapping() throws Exception {
        mockMvc.perform(get("/rest07/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceGrosze").value(2490));

        mockMvc.perform(get("/rest07/products/ping")).andExpect(content().string("pong"));
        mockMvc.perform(get("/rest07/products/ping").header(VERSION_HEADER, "2.0")).andExpect(content().string("pong"));
    }

    @Test
    @DisplayName("4. Wersja bazowa „1.1+”: 1.1 i 2.0 → 200, starsza 1.0 → 404 (dla niej tego endpointu nie ma)")
    void baselineVersion() throws Exception {
        mockMvc.perform(get("/rest07/products/1/stock").header(VERSION_HEADER, "1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(12));
        mockMvc.perform(get("/rest07/products/1/stock").header(VERSION_HEADER, "2.0"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest07/products/1/stock").header(VERSION_HEADER, "1.0"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Nieobsługiwana wersja 3.0 albo „abc” → 400 „Invalid API version”")
    void unsupportedVersion() throws Exception {
        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "3.0"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Invalid API version: '3.0.0'."))
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(InvalidApiVersionException.class));

        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Invalid API version: 'abc'."));
    }

    @Test
    @DisplayName("4. Wersja 1.1 jest obsługiwana (wykryta z „1.1+”), ale /products/{id} nie ma mapowania dla 1.1 → 400, nie 404")
    void supportedButNotMappedVersion() throws Exception {
        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(NotAcceptableApiVersionException.class));
    }

    @Test
    @DisplayName("4. Wycofywana wersja 1.0 (także jako domyślna) → nagłówki Deprecation, Sunset i Link; wersja 2.0 ich nie ma")
    void deprecationHeaders() throws Exception {
        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "1.0"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "@1767225600"))   // 2026-01-01T00:00Z jako sekundy od 1970
                .andExpect(header().string("Sunset", "Fri, 1 Jan 2027 00:00:00 GMT"))
                .andExpect(header().string("Link",
                        "<https://sklep.example.com/docs/api/migracja-v2>; rel=\"sunset\"; type=\"text/html\""));

        // bez nagłówka = wersja domyślna 1.0 = też wycofywana
        mockMvc.perform(get("/rest07/products/1")).andExpect(header().string("Deprecation", "@1767225600"));

        mockMvc.perform(get("/rest07/products/1").header(VERSION_HEADER, "2.0"))
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"));
    }
}
