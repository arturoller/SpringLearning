package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.MissingApiVersionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 3–4 lekcji Rest07ApiVersioning: TEN SAM ProductController, ale INNA strategia — wersja w parametrze zapytania
 * {@code ?api-version=…}. Bez profilu rest07, więc nie ma też wersji domyślnej: brak wersji = błąd 400.
 * Kontroler nie zmienił się ani o znak — strategia to sprawa konfiguracji.
 */
@WebMvcTest(properties = "spring.mvc.apiversion.use.query-parameter=api-version")
@Import(ProductCatalog.class)
class Rest07QueryParameterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("3. Wersja w parametrze zapytania: ?api-version=2.0 → kształt z wersji 2.0")
    void versionFromQueryParameter() throws Exception {
        mockMvc.perform(get("/rest07/products/1").queryParam("api-version", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.currency").value("PLN"));

        mockMvc.perform(get("/rest07/products/1?api-version=1.0"))
                .andExpect(jsonPath("$.priceGrosze").value(2490));
    }

    @Test
    @DisplayName("4. Brak wersji i brak wersji domyślnej → 400 „API version is required.” (wersja jest wtedy wymagana)")
    void missingVersionIsRequired() throws Exception {
        mockMvc.perform(get("/rest07/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("API version is required."))
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(MissingApiVersionException.class));

        // nagłówek nie jest teraz źródłem wersji — Spring go nie czyta
        mockMvc.perform(get("/rest07/products/1").header("API-Version", "2.0"))
                .andExpect(status().isBadRequest());
    }
}
