package s11_testing.test03_mock_mvc_tester;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 1–5 lekcji Test03MockMvcTester: ten sam kontroler sprawdzany klasycznym MockMvc i nowym MockMvcTester.
 * Wycinek {@code @WebMvcTest}: serwis to atrapa, więc każdy test sam ustala dane — nie zależy od innych testów.
 */
@WebMvcTest(ProductController.class)
class Test03MockMvcTesterTest {

    static final ProductResponse COFFEE =
            new ProductResponse("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99"));
    static final ProductResponse CHOCOLATE =
            new ProductResponse("SPO-002", "Czekolada gorzka", "SPOZYWCZE", new BigDecimal("7.49"));

    @Autowired
    MockMvc mockMvc;          // klasyczny „udawany” klient HTTP

    @Autowired
    MockMvcTester mvc;        // MockMvcTester = MockMvc z asercjami w stylu AssertJ (Spring Framework 6.2+)

    @Autowired
    ApplicationContext context;

    @MockitoBean
    ProductCatalogService service;

    @Test
    @DisplayName("1. Klasycznie: perform(get(...)).andExpect(status()...).andExpect(jsonPath(...))")
    void classicMockMvc() throws Exception {
        given(service.findBySku("SPO-001")).willReturn(Optional.of(COFFEE));

        mockMvc.perform(get("/api/products/SPO-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.name").value("Kawa ziarnista 1kg"))
                .andExpect(jsonPath("$.price").value(64.99));
    }

    @Test
    @DisplayName("2. MockMvcTester: Boot sam tworzy go w @WebMvcTest; assertThat(mvc.get().uri(...)).hasStatusOk()")
    void mockMvcTesterBasics() {
        given(service.findBySku("SPO-001")).willReturn(Optional.of(COFFEE));

        assertThat(context.getBeanNamesForType(MockMvcTester.class)).hasSize(1);   // bean z autokonfiguracji
        assertThat(mvc.get().uri("/api/products/{sku}", "SPO-001"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson().extractingPath("$.name").isEqualTo("Kawa ziarnista 1kg");
    }

    @Test
    @DisplayName("3. bodyJson(): ścieżki JSON, porównanie luźne (lenient) i zamiana na rekord (convertTo)")
    void bodyJson() {
        given(service.findAll("SPOZYWCZE")).willReturn(List.of(COFFEE, CHOCOLATE));

        var result = mvc.get().uri("/api/products").param("category", "SPOZYWCZE").exchange();

        assertThat(result).bodyJson().extractingPath("$[*].sku").asArray().containsExactly("SPO-001", "SPO-002");
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                [ {"sku": "SPO-001"}, {"sku": "SPO-002", "price": 7.49} ]
                """);   // luźno: brakujące pola w oczekiwanym JSON-ie nie przeszkadzają
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                [ {"sku": "SPO-002"}, {"sku": "SPO-001"} ]
                """);   // luźno: kolejność elementów tablicy też nie ma znaczenia
        assertThat(result).bodyJson().extractingPath("$[0]").convertTo(ProductResponse.class).isEqualTo(COFFEE);
    }

    @Test
    @DisplayName("3. isStrictlyEqualTo: ścisłe porównanie wymaga KAŻDEGO pola — ten sam niepełny JSON nie przechodzi")
    void strictComparisonNeedsAllFields() {
        given(service.findBySku("SPO-002")).willReturn(Optional.of(CHOCOLATE));
        var result = mvc.get().uri("/api/products/SPO-002").exchange();

        assertThatThrownBy(() -> assertThat(result).bodyJson().isStrictlyEqualTo("""
                {"sku": "SPO-002", "price": 7.49}
                """)).isInstanceOf(AssertionError.class);

        assertThat(result).bodyJson().isStrictlyEqualTo("""
                {"sku": "SPO-002", "name": "Czekolada gorzka", "category": "SPOZYWCZE", "price": 7.49}
                """);
    }

    @Test
    @DisplayName("4. POST z JSON-em: 201 Created, nagłówek Location, serwis dostał właściwe żądanie")
    void postCreatesProduct() {
        var request = new NewProductRequest("DOM-001", "Ekspres do kawy", "DOM", new BigDecimal("1899.00"));
        given(service.create(request)).willReturn(
                new ProductResponse("DOM-001", "Ekspres do kawy", "DOM", new BigDecimal("1899.00")));

        assertThat(mvc.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "DOM-001", "name": "Ekspres do kawy", "category": "DOM", "price": 1899.00}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/products/DOM-001")
                .bodyJson().extractingPath("$.sku").isEqualTo("DOM-001");
        verify(service).create(request);
    }

    @Test
    @DisplayName("4. Duplikat SKU → 409 Conflict z komunikatem; DELETE → 204 No Content")
    void conflictAndDelete() {
        given(service.create(new NewProductRequest("SPO-001", "Kawa", "SPOZYWCZE", new BigDecimal("1.00"))))
                .willThrow(new DuplicateSkuException("SPO-001"));
        given(service.delete("SPO-002")).willReturn(true);

        assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "SPO-001", "name": "Kawa", "category": "SPOZYWCZE", "price": 1.00}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.error").isEqualTo("Produkt o SKU SPO-001 już istnieje");
        assertThat(mvc.delete().uri("/api/products/SPO-002")).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("5. 404 z ResponseStatusException: MockMvc widzi status i komunikat błędu, ale ciało jest PUSTE")
    void notFound() {
        given(service.findBySku("XXX-999")).willReturn(Optional.empty());

        assertThat(mvc.get().uri("/api/products/XXX-999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasErrorMessage("Nie ma produktu XXX-999")
                .hasBodyTextEqualTo("");   // strony /error Boota w MockMvc nie ma (prawdziwy serwer: sekcja 6)
    }

    @Test
    @DisplayName("5. Nieobsłużony wyjątek: perform() RZUCA ServletException, a MockMvcTester zapisuje go jako porażkę")
    void unhandledException() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/products/SPO-001/reviews")))
                .isInstanceOf(ServletException.class)
                .hasMessage("Request processing failed: java.lang.IllegalStateException: "
                        + "Opinie o produktach nie są jeszcze gotowe");

        assertThat(mvc.get().uri("/api/products/SPO-001/reviews"))   // nie rzuca — wyjątek ląduje w wyniku
                .hasFailed()
                .failure()
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Opinie o produktach nie są jeszcze gotowe");
    }
}
