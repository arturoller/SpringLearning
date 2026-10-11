package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Rest07ApiVersioning: to samo BEZ kodu — właściwościami Boota spring.mvc.apiversion.*.
 * Własna, mała konfiguracja testu (PropertiesOnlyApp) bierze tylko ProductVersionedController, BEZ ApiVersioningConfig —
 * wersjonowanie włączają wyłącznie właściwości: wersja z parametru ?v=..., domyślna 1.0.
 */
@SpringBootTest(classes = Rest07ApiVersioningPropertiesTest.PropertiesOnlyApp.class, properties = {
        "spring.mvc.apiversion.use.query-parameter=v",
        "spring.mvc.apiversion.supported=1.0,1.1,2.0",
        "spring.mvc.apiversion.default=1.0"})
@AutoConfigureMockMvc
class Rest07ApiVersioningPropertiesTest {

    /**
     * Konfiguracja tylko dla tego testu: autokonfiguracja Boota + jeden kontroler, bez skanowania pakietu.
     * Zwykłe {@code @Configuration}: gdyby to była {@code @SpringBootConfiguration}, inne testy tego pakietu
     * znalazłyby DWIE klasy startowe (tę i Rest07Application); gdyby {@code @TestConfiguration} — Boot dołożyłby do niej
     * Rest07Application (a więc i ApiVersioningConfig). Klasy zagnieżdżone w testach Boot pomija przy skanowaniu.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(ProductVersionedController.class)
    static class PropertiesOnlyApp {
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("2. spring.mvc.apiversion.use.query-parameter=v: wersja z ?v=2.0")
    void versionFromQueryParameter() throws Exception {
        mockMvc.perform(get("/api/products/1?v=2.0"))
                .andExpect(content().json("{\"price\":{\"amount\":29.99}}"));
    }

    @Test
    @DisplayName("2. spring.mvc.apiversion.default=1.0: bez wersji → 1.0 zamiast 400")
    void defaultVersion() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"priceGrosze\":2999}"));
    }

    @Test
    @DisplayName("2. Nagłówek API-Version jest tu ignorowany — wersję czyta tylko parametr v")
    void headerIgnored() throws Exception {
        assertThat(context.getBeanNamesForType(ApiVersioningConfig.class)).isEmpty();   // tylko właściwości
        mockMvc.perform(get("/api/products/1").header("API-Version", "2.0"))
                .andExpect(content().json("{\"priceGrosze\":2999}"));   // domyślna 1.0, nie 2.0
    }
}
