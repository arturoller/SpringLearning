package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest07ApiVersioning. {@code @ActiveProfiles("rest07")} — włącza application-rest07.properties
 * (spring.mvc.apiversion.*), tak jak Rest07Application przy starcie. ApiDeprecationConfig wchodzi do wycinka sam,
 * bo implementuje WebMvcConfigurer. Wersję podajemy nagłówkiem API-Version (albo parametrem ?version=).
 */
@WebMvcTest(controllers = {VersionedProductController.class, ManualVersionController.class})
@ActiveProfiles("rest07")
class Rest07ApiVersioningTest {

    private static final String VERSION = "API-Version";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApiVersionStrategy strategy;   // bean mvcApiVersionStrategy zbudowany przez Spring MVC z właściwości i konfiguratorów

    @Test
    @DisplayName("1. Ręczne wersjonowanie: /api/v1/… i /api/v2/… to dwa zwykłe adresy")
    void manualVersioning() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/ELE-001")).andExpect(content().string("v1: ELE-001"));
        mockMvc.perform(get("/api/v2/catalog/ELE-001")).andExpect(content().string("v2: ELE-001 (z cenami netto i brutto)"));
    }

    @Test
    @DisplayName("2. Ten sam adres, różne wersje: API-Version 1.0 → ProductV1, 2.0 → ProductV2 (inny kształt JSON-a)")
    void sameAddressDifferentVersions() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(5499.99));

        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.net").value(4471.54))
                .andExpect(jsonPath("$.price.gross").value(5499.99))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("2. Wersja bazowa „1.0+”: 1.1 i 1.5 obsługuje metoda v1 — aż do 2.0, którą przejmuje „2.0+”")
    void baselineVersions() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "1.1")).andExpect(jsonPath("$.price").value(5499.99));
        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "1.5")).andExpect(jsonPath("$.price").value(5499.99));
        mockMvc.perform(get("/api/products").header(VERSION, "2.0"))   // lista „1.0+” obsługuje też 2.0
                .andExpect(jsonPath("$[0]").value("ELE-001"));
    }

    @Test
    @DisplayName("2. Adres dodany w 1.1 („1.1+”): w wersji 1.0 go NIE MA → 404 (pytanie 3)")
    void endpointAddedLater() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001/stock").header(VERSION, "1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(get("/api/products/ELE-001/stock").header(VERSION, "1.0"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(NoResourceFoundException.class));
    }

    @Test
    @DisplayName("2. Wersja STAŁA „1.0”: zapytanie 1.1 → 400 i NotAcceptableApiVersionException")
    void fixedVersionRejectsOthers() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001/legacy-price").header(VERSION, "1.0"))
                .andExpect(content().string("5499.99 PLN"));

        mockMvc.perform(get("/api/products/ELE-001/legacy-price").header(VERSION, "1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(NotAcceptableApiVersionException.class));
    }

    @Test
    @DisplayName("3. Źródła wersji: nagłówek, parametr ?version=; nagłówek wygrywa (jest pierwszy na liście)")
    void versionSources() throws Exception {
        // queryParam, a nie param: wersję czyta QueryApiVersionResolver z request.getQueryString(), a MockMvc
        // param() dodaje parametr BEZ query stringa (prawdziwy klient zawsze wysyła go w adresie)
        mockMvc.perform(get("/api/products/ELE-001").queryParam("version", "2.0"))
                .andExpect(jsonPath("$.price.gross").value(5499.99));

        mockMvc.perform(get("/api/products/ELE-001").queryParam("version", "1.0").header(VERSION, "2.0"))
                .andExpect(jsonPath("$.price.gross").value(5499.99));   // 2.0 z nagłówka
    }

    @Test
    @DisplayName("3. Parser wersji semantycznych: „1” = „1.0” = „1.0.0”, a „v2” to 2.0 (litery na początku są pomijane)")
    void semanticVersionParsing() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "1")).andExpect(jsonPath("$.price").value(5499.99));
        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "v2")).andExpect(jsonPath("$.price.gross").exists());
        assertThat(strategy.parseVersion("1")).isEqualTo(strategy.parseVersion("1.0.0"));
    }

    @Test
    @DisplayName("4. Brak wersji → wersja domyślna 1.0 (spring.mvc.apiversion.default)")
    void missingVersionUsesDefault() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(5499.99));
        assertThat(strategy.getDefaultVersion()).hasToString("1.0.0");
    }

    @Test
    @DisplayName("4. Nieznana wersja 3.0 → 400 „Invalid API version: '3.0.0'.”; zły format „abc” → 400 (pytanie 2)")
    void unsupportedAndInvalidVersions() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "3.0"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(InvalidApiVersionException.class);
                    assertThat(result.getResponse().getErrorMessage()).isEqualTo("Invalid API version: '3.0.0'.");
                });

        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResponse().getErrorMessage())
                        .isEqualTo("Invalid API version: 'abc'."));
    }

    @Test
    @DisplayName("4. 1.5 jest na liście supported (choć żadna metoda nie ma „1.5”) → obsłużona przez wersję bazową")
    void supportedButNotMapped() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001/stock").header(VERSION, "1.5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. PUŁAPKA: zła wersja psuje też adresy BEZ atrybutu version — /api/v1/catalog z API-Version: 3.0 → 400")
    void invalidVersionBreaksUnversionedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/ELE-001").header(VERSION, "3.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Wycofywana wersja 1.0 (także domyślna!) dostaje nagłówki Deprecation, Link i Sunset; 2.0 — nie")
    void deprecationHeaders() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001"))   // bez wersji = 1.0
                .andExpect(header().string("Deprecation", "@1788220800"))   // 2026-09-01T00:00Z w sekundach od 1970
                .andExpect(header().string("Link",
                        "<https://sklep.example/api/migracja-v2>; rel=\"deprecation\"; type=\"text/html\""))
                .andExpect(header().string("Sunset", "Wed, 31 Mar 2027 00:00:00 GMT"));

        mockMvc.perform(get("/api/products/ELE-001").header(VERSION, "2.0"))
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"));
    }
}
