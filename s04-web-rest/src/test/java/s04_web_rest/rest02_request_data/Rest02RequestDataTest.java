package s04_web_rest.rest02_request_data;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest02RequestData. {@code @WebMvcTest} z kontrolerami lekcji (bez szkieletu ćwiczeń) — wystarczy
 * warstwa webowa: kontrolery czytają dane z niezmiennej listy Product.ALL, nie potrzebują innych beanów.
 */
@WebMvcTest(controllers = {ProductQueryController.class, CategoryController.class, PitfallController.class})
class Rest02RequestDataTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. @PathVariable: {sku} z adresu trafia do parametru; konkretny adres /search wygrywa ze wzorcem /{sku}")
    void pathVariable() throws Exception {
        mockMvc.perform(get("/api/products/KSI-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java. Podstawy"));

        mockMvc.perform(get("/api/products/search").param("name", "czysty"))   // NIE one("search")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", contains("KSI-001")));
    }

    @Test
    @DisplayName("1. @PathVariable typu enum i dwie zmienne ścieżki z nazwami podanymi wprost")
    void enumAndTwoPathVariables() throws Exception {
        mockMvc.perform(get("/api/categories/KSIAZKI/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", contains("KSI-001", "KSI-002", "KSI-003")));

        mockMvc.perform(get("/api/categories/KSIAZKI/products/KSI-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java. Podstawy"));
    }

    @Test
    @DisplayName("1. Enum w ścieżce rozróżnia wielkość liter: /categories/ksiazki → 400 (pytanie 6)")
    void enumPathVariableIsCaseSensitive() throws Exception {
        mockMvc.perform(get("/api/categories/ksiazki/products"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MethodArgumentTypeMismatchException.class));
    }

    @Test
    @DisplayName("2. @RequestParam z defaultValue: bez parametrów limit 5; ?limit=3&offset=2 — trzy produkty od trzeciego")
    void requestParamWithDefaults() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        mockMvc.perform(get("/api/products").param("limit", "3").param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", contains("ELE-001", "ELE-002", "ELE-003")));   // po DOM-001, DOM-002
    }

    @Test
    @DisplayName("2. Brak wymaganego @RequestParam → 400 i MissingServletRequestParameterException")
    void missingRequiredParam() throws Exception {
        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingServletRequestParameterException.class)
                        .hasMessage("Required request parameter 'name' for method parameter type String is not present"));
    }

    @Test
    @DisplayName("2. Optional<Category>: bez parametru — wszystkie kategorie, z parametrem — tylko ta jedna")
    void optionalParam() throws Exception {
        mockMvc.perform(get("/api/products/cheaper-than").param("max", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku",
                        contains("KSI-001", "KSI-003", "ODZ-002", "SPO-001", "SPO-002", "SPO-003")));

        mockMvc.perform(get("/api/products/cheaper-than").param("max", "100").param("category", "KSIAZKI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", contains("KSI-001", "KSI-003")));
    }

    @Test
    @DisplayName("2. List<String>: ?sku=A&sku=B i ?sku=A,B dają tę samą listę")
    void listParam() throws Exception {
        mockMvc.perform(get("/api/products/by-sku").param("sku", "ELE-001", "KSI-001"))
                .andExpect(jsonPath("$[*].sku", contains("ELE-001", "KSI-001")));

        mockMvc.perform(get("/api/products/by-sku?sku=ELE-001,KSI-001"))
                .andExpect(jsonPath("$[*].sku", contains("ELE-001", "KSI-001")));
    }

    @Test
    @DisplayName("2. PUŁAPKA: pusty parametr ?name= to nie brak parametru — wyszukiwanie zwraca wszystkie 14 produktów")
    void emptyParamIsNotMissing() throws Exception {
        mockMvc.perform(get("/api/products/search?name="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)));
    }

    @Test
    @DisplayName("2. PUŁAPKA: required = false + int → 500 i IllegalStateException, nie 400 (pytanie 3)")
    void primitiveOptionalParameterGives500() {
        // MockMvc nie zamienia nieobsłużonego wyjątku na 500 — rzuca go dalej (prawdziwy serwer odpowie 500)
        assertThatThrownBy(() -> mockMvc.perform(get("/api/pitfalls/primitive")))
                .isInstanceOf(ServletException.class)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Optional int parameter 'limit' is present but cannot be translated into a null value");
    }

    @Test
    @DisplayName("3. Konwersja typów: ?limit=abc → 400 i MethodArgumentTypeMismatchException (pytanie 2)")
    void typeConversionErrorGives400() throws Exception {
        mockMvc.perform(get("/api/products").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MethodArgumentTypeMismatchException.class)
                        .hasMessage("Method parameter 'limit': Failed to convert value of type 'java.lang.String' "
                                + "to required type 'int'; For input string: \"abc\""));
    }

    @Test
    @DisplayName("ZNAJDŹ BŁĄD: @PathVariable(\"sku\") przy zmiennej {id} → 500 i MissingPathVariableException (pytanie 4)")
    void wrongPathVariableNameGives500() throws Exception {
        mockMvc.perform(get("/api/pitfalls/products/ELE-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingPathVariableException.class));
    }

    @Test
    @DisplayName("4. @RequestBody: JSON z ciała → rekord PriceCheckRequest; odpowiedź z wyceną")
    void requestBody() throws Exception {
        mockMvc.perform(post("/api/products/price-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"KSI-001","quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(79.00))
                .andExpect(jsonPath("$.total").value(237.00));
    }

    @Test
    @DisplayName("4. Brak ciała, zły JSON, brak pola int → 400 i HttpMessageNotReadableException")
    void requestBodyErrors() throws Exception {
        mockMvc.perform(post("/api/products/price-check").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpMessageNotReadableException.class)
                        .hasMessageStartingWith("Required request body is missing"));

        mockMvc.perform(post("/api/products/price-check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"KSI-001\",\"quantity\":}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageStartingWith("JSON parse error: Unexpected character ('}' (code 125)): expected a value"));

        mockMvc.perform(post("/api/products/price-check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"KSI-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageStartingWith("JSON parse error: Cannot map `null` into type `int`"));
    }

    @Test
    @DisplayName("5. @RequestHeader z defaultValue: bez nagłówka po polsku, z Accept-Language: en-US… po angielsku")
    void requestHeaderWithDefault() throws Exception {
        String polish = mockMvc.perform(get("/api/products/ELE-001/label"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(polish).isEqualTo("Laptop Pro 14 — 5499.99 zł");

        mockMvc.perform(get("/api/products/ELE-001/label").header("accept-language", "en-US,en;q=0.9"))
                .andExpect(content().string("Laptop Pro 14 - PLN 5499.99"));   // nazwa nagłówka małymi literami też działa
    }

    @Test
    @DisplayName("5. Brak wymaganego nagłówka X-Shop-Id → 400 i MissingRequestHeaderException")
    void missingRequiredHeader() throws Exception {
        mockMvc.perform(get("/api/products/stock-report"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingRequestHeaderException.class)
                        .hasMessage("Required request header 'X-Shop-Id' for method parameter type String is not present"));

        String report = mockMvc.perform(get("/api/products/stock-report").header("X-Shop-Id", "WAW-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(report).isEqualTo("Sklep WAW-01: 14 produktów, niedostępne: 2");
    }

    @Test
    @DisplayName("7. @ModelAttribute: parametry zapytania → rekord ProductFilter; brak parametru = null = bez filtra")
    void modelAttributeFromQueryParams() throws Exception {
        mockMvc.perform(get("/api/products/filter")
                        .param("category", "ELEKTRONIKA").param("maxPrice", "1500").param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", contains("ELE-003", "ELE-004")));

        mockMvc.perform(get("/api/products/filter"))
                .andExpect(jsonPath("$", hasSize(14)));
    }

    @Test
    @DisplayName("7. Błąd konwersji w @ModelAttribute → 400, ale MethodArgumentNotValidException z błędem pola")
    void modelAttributeConversionError() throws Exception {
        mockMvc.perform(get("/api/products/filter").param("maxPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MethodArgumentNotValidException.class)
                        .hasMessageContaining("Field error in object 'productFilter' on field 'maxPrice': rejected value [abc]"));
    }
}
