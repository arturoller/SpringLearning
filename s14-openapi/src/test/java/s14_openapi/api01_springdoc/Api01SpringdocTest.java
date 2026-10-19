package s14_openapi.api01_springdoc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Api01Springdoc — „wynik” każdej sekcji. Wybrany wycinek: {@code @SpringBootTest} + MockMvc, bo springdoc
 * jest zwykłym zestawem beanów (autokonfiguracją) i potrzebuje CAŁEGO kontekstu; {@code @WebMvcTest} nie ładuje
 * autokonfiguracji springdoc. Zapytania idą przez MockMvc, więc nie ma portu ani sieci.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Api01SpringdocTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Sekcja 1: bez żadnych adnotacji springdoc buduje dokument OpenAPI 3.1 z domyślnymi informacjami")
    void documentExistsWithoutAnnotations() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.title").value("OpenAPI definition"))
                .andExpect(jsonPath("$.info.version").value("v0"));
    }

    @Test
    @DisplayName("Sekcja 2: ścieżki i operacje pochodzą z adnotacji Spring MVC; nazwa metody to operationId")
    void pathsAndOperationsComeFromController() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths", aMapWithSize(2)))
                .andExpect(jsonPath("$.paths['/api/products']", hasKey("get")))
                .andExpect(jsonPath("$.paths['/api/products']", hasKey("post")))
                .andExpect(jsonPath("$.paths['/api/products/{sku}']", hasKey("get")))
                .andExpect(jsonPath("$.paths['/api/products/{sku}']", hasKey("delete")))
                .andExpect(jsonPath("$.paths['/api/products'].get.operationId").value("all"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].delete.operationId").value("delete"))
                // kontroler bez żadnej adnotacji dokumentującej: tag = nazwa klasy w stylu kebab-case
                .andExpect(jsonPath("$.paths['/api/products'].get.tags[0]").value("product-controller"));
    }

    @Test
    @DisplayName("Sekcja 3: parametry — zmienna ze ścieżki jest wymagana, parametr zapytania z required=false nie")
    void parametersAreReadFromMethodSignature() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[0].name").value("category"))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[0].in").value("query"))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[0].required").value(false))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.parameters[0].in").value("path"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.parameters[0].required").value(true));
    }

    @Test
    @DisplayName("Sekcja 4: rekordy z sygnatur stają się schematami; BigDecimal to number, int to integer/int32")
    void recordsBecomeSchemas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas", aMapWithSize(2)))
                .andExpect(jsonPath("$.components.schemas.Product.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.Product.properties.price.type").value("number"))
                .andExpect(jsonPath("$.components.schemas.Product.properties.stock.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.Product.properties.stock.format").value("int32"))
                .andExpect(jsonPath("$.components.schemas.Product.properties.sku.type").value("string"))
                // ciało zapytania POST: wymagane i wskazuje na schemat NewProduct
                .andExpect(jsonPath("$.paths['/api/products'].post.requestBody.required").value(true))
                .andExpect(jsonPath("$.paths['/api/products'].post.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/NewProduct"))
                // bez adnotacji walidacji żadne pole nie jest oznaczone jako wymagane
                .andExpect(jsonPath("$.components.schemas.NewProduct", not(hasKey("required"))));
    }

    @Test
    @DisplayName("Sekcja 5: PUŁAPKA — dokument mówi 200, a kontroler naprawdę zwraca 201 (i 404, o którym dokument milczy)")
    void documentCannotSeeStatusChosenInsideMethodBody() throws Exception {
        // 1) prawdziwa odpowiedź kontrolera
        mvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                        .content("""
                                {"sku":"DOM-002","name":"Lampka biurkowa","category":"DOM","price":129.00,"stock":18}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/DOM-002")));
        mvc.perform(get("/api/products/NIE-MA")).andExpect(status().isNotFound());

        // 2) to, co o tym samym zapytaniu mówi dokument: springdoc nie wykonuje kodu, więc nie wie, że
        // ResponseEntity.created(...) da 201 — zakłada „200 OK”
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/products'].post.responses", hasKey("200")))
                .andExpect(jsonPath("$.paths['/api/products'].post.responses", not(hasKey("201"))))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses", aMapWithSize(1)))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses", not(hasKey("404"))))
                // wyjątek: stały status z @ResponseStatus springdoc widzi
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].delete.responses", hasKey("204")));
    }

    @Test
    @DisplayName("Sekcja 6: Swagger UI jest pod /swagger-ui.html (przekierowanie), a ten sam dokument jest też jako YAML")
    void swaggerUiAndYaml() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        // UI pobiera dokument adresem zapisanym w swagger-config
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
        mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.oai.openapi"));
    }

    // --- Sekcja 7: właściwości springdoc.* — każda w osobnym kontekście ---

    @Nested
    @SpringBootTest(properties = {"springdoc.api-docs.path=/docs/openapi", "springdoc.swagger-ui.path=/docs/ui.html"})
    @AutoConfigureMockMvc
    @DisplayName("Sekcja 7a: własne ścieżki dokumentu i UI")
    class CustomPaths {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("Dokument i UI są pod nowymi adresami, a pod starymi — 404")
        void movedPaths() throws Exception {
            mvc.perform(get("/docs/openapi")).andExpect(status().isOk());
            mvc.perform(get("/docs/openapi.yaml")).andExpect(status().isOk());
            mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
            // UWAGA: zasoby UI przenoszą się pod ten sam prefiks, co podana ścieżka
            mvc.perform(get("/docs/ui.html"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "/docs/swagger-ui/index.html"));
        }
    }

    @Nested
    @SpringBootTest(properties = "springdoc.swagger-ui.enabled=false")
    @AutoConfigureMockMvc
    @DisplayName("Sekcja 7b: wyłączony tylko Swagger UI")
    class UiDisabled {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("Dokument nadal działa, strona UI znika (404)")
        void documentRemainsUiGone() throws Exception {
            mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
            mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
            mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("api01prod")
    @DisplayName("Sekcja 7c: profil produkcyjny z springdoc.api-docs.enabled=false")
    class ProductionProfile {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("Wyłączenie dokumentu wyłącza też Swagger UI — oba adresy dają 404")
        void everythingGone() throws Exception {
            mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
            // sama aplikacja działa dalej
            mvc.perform(get("/api/products")).andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(properties = {"springdoc.paths-to-match=/api/products/{sku}",
            "springdoc.default-produces-media-type=application/json"})
    @AutoConfigureMockMvc
    @DisplayName("Sekcja 7d: filtr ścieżek i domyślny typ odpowiedzi")
    class PathFilter {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("W dokumencie zostaje tylko pasująca ścieżka, a typ odpowiedzi to application/json zamiast */*")
        void onlyMatchingPaths() throws Exception {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths", aMapWithSize(1)))
                    .andExpect(jsonPath("$.paths", hasKey("/api/products/{sku}")))
                    .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses['200'].content",
                            hasKey("application/json")));
        }
    }
}
