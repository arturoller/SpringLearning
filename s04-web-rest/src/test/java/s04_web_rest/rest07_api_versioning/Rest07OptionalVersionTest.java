package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 4 lekcji Rest07ApiVersioning: wersja NIEwymagana ({@code required=false}) i BEZ wersji domyślnej (PUŁAPKA)
 * oraz lista {@code supported} — wersje obsługiwane dopisane ręcznie do tych wykrytych z mapowań.
 */
@WebMvcTest(properties = {
        "spring.mvc.apiversion.use.header=API-Version",
        "spring.mvc.apiversion.required=false",
        "spring.mvc.apiversion.supported=1.5"})
@Import(ProductCatalog.class)
class Rest07OptionalVersionTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("4. PUŁAPKA: required=false bez wersji domyślnej → zapytanie bez wersji trafia do NAJNOWSZEJ wersji (2.0)")
    void noVersionMeansLatest() throws Exception {
        mockMvc.perform(get("/rest07/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.currency").value("PLN"))
                .andExpect(jsonPath("$.priceGrosze").doesNotExist());
    }

    @Test
    @DisplayName("4. supported=1.5: wersja 1.5 jest przyjmowana (pasuje do „1.1+”), 1.7 nie jest na żadnej liście → 400")
    void supportedVersions() throws Exception {
        mockMvc.perform(get("/rest07/products/1/stock").header("API-Version", "1.5"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest07/products/1/stock").header("API-Version", "1.7"))
                .andExpect(status().isBadRequest());
    }
}
