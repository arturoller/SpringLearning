package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Testy ĆWICZEŃ lekcji Test03MockMvcTester. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Atrapa serwisu jest już nauczona (metoda {@code setUp}). Ty piszesz zapytanie przez {@code mvc} (albo oczekiwany
 * JSON) w miejscu {@code todo()}. Asercje na końcu metod są gotowe — nie zmieniaj ich.
 */
@Tag("cwiczenie")
@WebMvcTest(ProductController.class)
class Test03MockMvcTesterExercisesTest {

    static final ProductResponse CLEAN_CODE =
            new ProductResponse("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"));
    static final ProductResponse JAVA_BASICS =
            new ProductResponse("KSI-002", "Java. Podstawy", "KSIAZKI", new BigDecimal("129.00"));
    static final NewProductRequest JACKET =
            new NewProductRequest("ODZ-001", "Kurtka zimowa", "ODZIEZ", new BigDecimal("459.00"));

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie zastąpisz swoim kodem. */
    static <T> T todo() {
        throw new UnsupportedOperationException("TODO");
    }

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
    @DisplayName("Ćwiczenie 1: GET jednego produktu przez MockMvcTester")
    void exercise1() {
        MvcTestResult result = todo();   // TODO: wyślij GET /api/products/KSI-001

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Czysty kod");
    }

    @Test
    @DisplayName("Ćwiczenie 2: GET listy z parametrem category=KSIAZKI")
    void exercise2() {
        MvcTestResult result = todo();   // TODO: wyślij GET /api/products z parametrem category=KSIAZKI

        assertThat(result).hasStatusOk()
                .bodyJson().extractingPath("$[*].sku").asArray().containsExactly("KSI-001", "KSI-002");
    }

    @Test
    @DisplayName("Ćwiczenie 3: POST nowego produktu z ciałem JSON → 201 i Location")
    void exercise3() {
        MvcTestResult result = todo();   // TODO: wyślij POST /api/products z JSON-em kurtki (treść w klasie lekcji)

        assertThat(result).hasStatus(HttpStatus.CREATED).hasHeader("Location", "/api/products/ODZ-001");
        verify(service).create(JACKET);
    }

    @Test
    @DisplayName("Ćwiczenie 4: pełny JSON produktu KSI-002 przechodzi porównanie ścisłe")
    void exercise4() {
        String expectedJson = todo();   // TODO: wpisz pełny JSON produktu KSI-002 (wszystkie pola)

        assertThat(mvc.get().uri("/api/products/KSI-002")).bodyJson().isStrictlyEqualTo(expectedJson);
    }
}
