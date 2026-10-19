package s11_testing.solutions.test03_mock_mvc_tester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import s11_testing.test03_mock_mvc_tester.NewProductRequest;
import s11_testing.test03_mock_mvc_tester.ProductCatalogService;
import s11_testing.test03_mock_mvc_tester.ProductController;
import s11_testing.test03_mock_mvc_tester.ProductResponse;
import s11_testing.test03_mock_mvc_tester.Test03Application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test03MockMvcTester (te same testy co w Test03MockMvcTesterExercisesTest,
 * już uzupełnione). {@code @Tag("wzorzec")} — biegną zawsze. Zajrzyj dopiero po własnej próbie!
 * Pakiet {@code solutions…} nie leży pod pakietem lekcji, więc konfigurację wskazujemy jawnie.
 */
@Tag("wzorzec")
@WebMvcTest(ProductController.class)
@ContextConfiguration(classes = Test03Application.class)
class Test03MockMvcTesterSolutionsTest {

    static final ProductResponse CLEAN_CODE =
            new ProductResponse("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"));
    static final ProductResponse JAVA_BASICS =
            new ProductResponse("KSI-002", "Java. Podstawy", "KSIAZKI", new BigDecimal("129.00"));
    static final NewProductRequest JACKET =
            new NewProductRequest("ODZ-001", "Kurtka zimowa", "ODZIEZ", new BigDecimal("459.00"));

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    ProductCatalogService service;

    @BeforeEach
    void setUp() {
        given(service.findBySku("KSI-001")).willReturn(Optional.of(CLEAN_CODE));
        given(service.findBySku("KSI-002")).willReturn(Optional.of(JAVA_BASICS));
        given(service.findAll("KSIAZKI")).willReturn(List.of(CLEAN_CODE, JAVA_BASICS));
        given(service.create(JACKET)).willReturn(
                new ProductResponse("ODZ-001", "Kurtka zimowa", "ODZIEZ", new BigDecimal("459.00")));
    }

    @Test
    @DisplayName("Wzorzec 1: GET jednego produktu przez MockMvcTester")
    void solution1() {
        MvcTestResult result = mvc.get().uri("/api/products/{sku}", "KSI-001").exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Czysty kod");
    }

    @Test
    @DisplayName("Wzorzec 2: GET listy z parametrem category=KSIAZKI")
    void solution2() {
        MvcTestResult result = mvc.get().uri("/api/products").param("category", "KSIAZKI").exchange();

        assertThat(result).hasStatusOk()
                .bodyJson().extractingPath("$[*].sku").asArray().containsExactly("KSI-001", "KSI-002");
    }

    @Test
    @DisplayName("Wzorzec 3: POST nowego produktu z ciałem JSON → 201 i Location")
    void solution3() {
        MvcTestResult result = mvc.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "ODZ-001", "name": "Kurtka zimowa", "category": "ODZIEZ", "price": 459.00}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED).hasHeader("Location", "/api/products/ODZ-001");
        verify(service).create(JACKET);
    }

    @Test
    @DisplayName("Wzorzec 4: pełny JSON produktu KSI-002 przechodzi porównanie ścisłe")
    void solution4() {
        String expectedJson = """
                {"sku": "KSI-002", "name": "Java. Podstawy", "category": "KSIAZKI", "price": 129.00}
                """;

        assertThat(mvc.get().uri("/api/products/KSI-002")).bodyJson().isStrictlyEqualTo(expectedJson);
    }
}
