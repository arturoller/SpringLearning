package s14_openapi.api03_groups_and_versions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Sekcja 7 lekcji Api03GroupsAndVersions: wersja API w parametrze zapytania zamiast w nagłówku. Osobna klasa BEZ profilu
 * "api03" (profil ustawia nagłówek), żeby strategia wersjonowania była tylko jedna.
 */
@SpringBootTest(properties = {"spring.mvc.apiversion.use.query-parameter=version", "spring.mvc.apiversion.default=1.0"})
@AutoConfigureMockMvc
class Api03GroupsAndVersionsQueryParamTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Sekcja 7: springdoc dokumentuje parametr zapytania \"version\" — strategię bierze z konfiguracji Springa")
    void queryParameterIsDocumented() throws Exception {
        mvc.perform(get("/v3/api-docs/v2"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.parameters[?(@.name == 'version')].in")
                        .value("query"));
        // parametr podajemy w adresie — samo .param(...) na żądaniu MockMvc nie zostało tu odczytane jako wersja API
        mvc.perform(get("/api/products/ELE-001?version=2.0"))
                .andExpect(jsonPath("$.price.gross").value(5499.99));
    }
}
