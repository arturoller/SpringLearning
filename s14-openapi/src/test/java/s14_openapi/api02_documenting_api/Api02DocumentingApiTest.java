package s14_openapi.api02_documenting_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import s14_openapi.api02_demo.StatusAdviceDemo;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Api02DocumentingApi. Wycinek: {@code @SpringBootTest} + MockMvc (jak w Api01Springdoc — springdoc
 * potrzebuje pełnego kontekstu). Test „pilnujący dokumentu” to sekcja 6.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Api02DocumentingApiTest {

    static final String VALID = """
            {"sku":"DOM-002","name":"Lampka biurkowa","category":"DOM","price":129.00,"stock":18}""";
    static final String INVALID = """
            {"sku":"x","name":"","price":0,"stock":-1}""";

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Sekcja 1: @Tag, @Operation i @Parameter dodają polskie opisy, a znacznik zastępuje nazwę klasy")
    void operationTagAndParameterDescriptions() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.tags[0].name").value("Produkty"))
                .andExpect(jsonPath("$.tags[0].description").value("Katalog produktów sklepu"))
                .andExpect(jsonPath("$.paths['/api/products'].get.tags[0]").value("Produkty"))
                .andExpect(jsonPath("$.paths['/api/products'].get.summary").value("Lista produktów"))
                .andExpect(jsonPath("$.paths['/api/products'].get.description")
                        .value("Zwraca wszystkie produkty albo tylko z jednej kategorii."))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[0].description")
                        .value("Nazwa kategorii do przefiltrowania"))
                .andExpect(jsonPath("$.paths['/api/products'].get.parameters[0].example").value("KSIAZKI"))
                // operationId nadal pochodzi z nazwy metody — adnotacje go nie zmieniają
                .andExpect(jsonPath("$.paths['/api/products'].get.operationId").value("all"));
    }

    @Test
    @DisplayName("Sekcja 2: @Schema na polach rekordu daje opis, przykład i listę dozwolonych wartości (enum)")
    void schemaAnnotationAddsDescriptionAndExample() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.NewProduct.description").value("Dane nowego produktu sklepu"))
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.sku.example").value("ELE-001"))
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.price.example").value(5499.99))
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.category.enum[0]").value("ELEKTRONIKA"))
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.category.enum.length()").value(5));

        // PUŁAPKA: allowableValues to tylko opis — kategoria spoza listy przechodzi walidację i jest przyjęta (201)
        mvc.perform(post("/api/products").contentType(APPLICATION_JSON)
                        .content("""
                                {"sku":"INN-001","name":"Coś innego","category":"INNA","price":1.00,"stock":1}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Sekcja 3: ograniczenia Bean Validation trafiają do schematu (required, minLength, pattern, minimum...)")
    void beanValidationConstraintsAppearInSchema() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                // @NotNull / @NotBlank → pole na liście "required"; pole int (prymityw) z samym @Min — NIE
                .andExpect(jsonPath("$.components.schemas.NewProduct.required",
                        containsInAnyOrder("sku", "name", "category", "price")))
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.sku.minLength").value(1))     // @NotBlank
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.sku.pattern").value("[A-Z]{3}-\\d{3}")) // @Pattern
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.name.minLength").value(3))    // @Size(min=3)
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.name.maxLength").value(60))   // @Size(max=60)
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.price.minimum").value(0.01))  // @DecimalMin
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.stock.minimum").value(0))     // @Min
                .andExpect(jsonPath("$.components.schemas.NewProduct.properties.stock.maximum").value(10000)); // @Max
    }

    @Test
    @DisplayName("Sekcja 4: @ApiResponse dopisuje statusy 201, 400 i 404 — i są one zgodne z prawdziwymi odpowiedziami")
    void apiResponsesMatchRealBehaviour() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/products'].post.responses", hasKey("201")))
                .andExpect(jsonPath("$.paths['/api/products'].post.responses", not(hasKey("200"))))
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['201'].description")
                        .value("Produkt utworzony"))
                .andExpect(jsonPath("$.paths['/api/products'].post.responses['400'].content['application/problem+json']"
                        + ".schema['$ref']").value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses['404'].description")
                        .value("Nie ma produktu o takim kodzie"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties", hasKey("detail")))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties", hasKey("status")));

        // prawdziwe błędy to ProblemDetail (application/problem+json) — dokument nie kłamie o typie ciała
        mvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(INVALID))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/products/ZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Nie ma produktu ZZZ"));
    }

    @Test
    @DisplayName("Sekcja 5: @Hidden usuwa punkt końcowy z dokumentu, ale on nadal działa")
    void hiddenEndpointWorksButIsNotDocumented() throws Exception {
        mvc.perform(get("/api/products/internal/count")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths", aMapWithSize(2)))
                .andExpect(jsonPath("$.paths", not(hasKey("/api/products/internal/count"))));
    }

    @Test
    @DisplayName("Sekcja 6: test pilnujący dokumentu — każdy prawdziwy status odpowiedzi musi być w dokumencie")
    void documentStaysInSyncWithBehaviour() throws Exception {
        JsonNode doc = JsonMapper.builder().build()
                .readTree(mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());

        // prawdziwe wywołania: (metoda, wzorzec ścieżki z dokumentu, status faktycznie zwrócony)
        int created = mvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(VALID))
                .andReturn().getResponse().getStatus();
        int invalid = mvc.perform(post("/api/products").contentType(APPLICATION_JSON).content(INVALID))
                .andReturn().getResponse().getStatus();
        int found = mvc.perform(get("/api/products/ELE-001")).andReturn().getResponse().getStatus();
        int missing = mvc.perform(get("/api/products/ZZZ")).andReturn().getResponse().getStatus();
        int list = mvc.perform(get("/api/products")).andReturn().getResponse().getStatus();

        assertThat(documented(doc, "post", "/api/products", created)).as("POST 201").isTrue();
        assertThat(documented(doc, "post", "/api/products", invalid)).as("POST 400").isTrue();
        assertThat(documented(doc, "get", "/api/products/{sku}", found)).as("GET 200").isTrue();
        assertThat(documented(doc, "get", "/api/products/{sku}", missing)).as("GET 404").isTrue();
        assertThat(documented(doc, "get", "/api/products", list)).as("GET lista 200").isTrue();
        // i odwrotnie: gdyby ktoś zmienił kod na 200, a zostawił dokument — ten test by to wykrył
        assertThat(documented(doc, "post", "/api/products", 200)).as("POST 200 NIE jest opisany").isFalse();
    }

    private static boolean documented(JsonNode doc, String method, String path, int status) {
        JsonNode responses = doc.get("paths").get(path).get(method).get("responses");
        return responses.get(String.valueOf(status)) != null;
    }

    // --- Sekcja 7: obsługa wyjątku z @ResponseStatus trafia do WSZYSTKICH operacji ---

    @Nested
    @SpringBootTest(classes = Api02Application.class)
    @Import(StatusAdviceDemo.class)
    @AutoConfigureMockMvc
    @DisplayName("Sekcja 7: @ExceptionHandler z @ResponseStatus w @RestControllerAdvice")
    class StatusOnAdvice {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("Springdoc dopisuje taki status do KAŻDEJ operacji, także tam, gdzie wyjątek nie może wystąpić")
        void statusAppearsEverywhere() throws Exception {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['/api/products'].get.responses", hasKey("409")))
                    .andExpect(jsonPath("$.paths['/api/products'].post.responses", hasKey("409")))
                    .andExpect(jsonPath("$.paths['/api/products/{sku}'].get.responses", hasKey("409")))
                    .andExpect(jsonPath("$.paths['/api/products'].get.responses['409'].content['application/problem+json']")
                            .exists());
        }
    }

    @Test
    @DisplayName("Sekcja 7 (kontrola): w zwykłej aplikacji lekcji — bez takiej obsługi — status 409 nie pojawia się w dokumencie")
    void noGlobalStatusWithoutResponseStatusAnnotation() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/products'].get.responses", not(hasKey("409"))))
                // 404 z naszego ProductErrorAdvice (bez @ResponseStatus) NIE jest dopisywane globalnie
                .andExpect(jsonPath("$.paths['/api/products'].get.responses", not(hasKey("404"))));
    }
}
