package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Test03MockMvcTester (PUŁAPKA): kontroler BEZ własnej obsługi błędów. {@code @WebMvcTest} domyślnie
 * ładuje każdy {@code @RestControllerAdvice}; tu go wyłączamy ({@code excludeFilters}), żeby zobaczyć, co dostaje
 * klient bez advice: 400 z PUSTĄ treścią (nie wie, które pole jest złe) i wyjątek zamiast 404 dla brakującego produktu.
 */
@WebMvcTest(controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ProductErrorAdvice.class))
@Import(ProductService.class)
class Test03NoAdviceTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ProductService service;

    @BeforeEach
    void reset() {
        service.reset();
    }

    @Test
    @DisplayName("Sekcja 3: bez advice błąd walidacji to 400 z PUSTĄ treścią — klient nie dowie się, co poprawić")
    void validationErrorWithoutAdvice() {
        assertThat(mvc.post().uri("/test03/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"price\":-1}"))
                .hasStatus(400)
                .bodyText().isEmpty();
    }

    @Test
    @DisplayName("Sekcja 3: bez advice brak produktu NIE jest 404 — wyjątek ProductNotFoundException wychodzi z kontrolera")
    void notFoundWithoutAdvice() {
        assertThat(mvc.get().uri("/test03/products/99"))
                .hasFailed()
                .failure().hasRootCauseInstanceOf(ProductNotFoundException.class);
    }
}
