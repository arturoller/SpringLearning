package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcje 1, 2 i 5 lekcji Err04ProblemDetail — zwykłe testy jednostkowe, BEZ Springa: {@code ProblemDetail} i wyjątki
 * {@code ErrorResponse} to zwykłe klasy Javy, więc nie potrzebujemy kontekstu ani MockMvc.
 */
class Err04ProblemDetailAnatomyTest {

    @Test
    @DisplayName("Sekcja 1: ProblemDetail ma pola type, title, status, detail, instance oraz własne właściwości")
    void problemDetailFields() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Nie ma produktu o id 9");

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Nie ma produktu o id 9");
        assertThat(problem.getType()).isNull();                       // brak type = w JSON go nie będzie (RFC: about:blank)
        assertThat(problem.getInstance()).isNull();                   // instance ustawia za nas Spring przy wysyłaniu
        assertThat(problem.getTitle()).isEqualTo("Not Found");        // brak title = opis statusu po angielsku
        assertThat(problem.getProperties()).isNull();                 // brak własnych właściwości

        problem.setTitle("Produkt nie istnieje");
        problem.setType(URI.create("https://sklep.example/errors/product-not-found"));
        problem.setProperty("code", "PRODUCT_NOT_FOUND");
        assertThat(problem.getTitle()).isEqualTo("Produkt nie istnieje");
        assertThat(problem.getType()).hasToString("https://sklep.example/errors/product-not-found");
        assertThat(problem.getProperties()).containsEntry("code", "PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("Sekcja 2: ResponseStatusException i ErrorResponseException to ErrorResponse — niosą gotowy ProblemDetail")
    void errorResponsesCarryProblemDetail() {
        ResponseStatusException statusException = new ResponseStatusException(HttpStatus.CONFLICT, "Produkt 1 jest już zarezerwowany");
        ProductArchivedException archived = new ProductArchivedException(5);

        assertThat(statusException).isInstanceOf(ErrorResponse.class);
        assertThat(statusException.getStatusCode().value()).isEqualTo(409);
        assertThat(statusException.getBody().getDetail()).isEqualTo("Produkt 1 jest już zarezerwowany");

        assertThat(archived).isInstanceOf(ErrorResponse.class);
        assertThat(archived.getBody().getStatus()).isEqualTo(410);
        assertThat(archived.getBody().getDetail()).isEqualTo("Produkt 5 został wycofany z oferty");
        assertThat(archived.getBody().getProperties()).containsEntry("code", "PRODUCT_ARCHIVED");
    }

    @Test
    @DisplayName("Sekcja 5: kody komunikatów z MessageSource mają postać problemDetail.type|title|<nazwa klasy wyjątku>")
    void messageCodes() {
        assertThat(ErrorResponse.getDefaultTypeMessageCode(HttpRequestMethodNotSupportedException.class))
                .isEqualTo("problemDetail.type.org.springframework.web.HttpRequestMethodNotSupportedException");
        assertThat(ErrorResponse.getDefaultTitleMessageCode(HttpRequestMethodNotSupportedException.class))
                .isEqualTo("problemDetail.title.org.springframework.web.HttpRequestMethodNotSupportedException");
        assertThat(ErrorResponse.getDefaultDetailMessageCode(HttpRequestMethodNotSupportedException.class, null))
                .isEqualTo("problemDetail.org.springframework.web.HttpRequestMethodNotSupportedException");
        assertThat(new ProductArchivedException(5).getDetailMessageCode())
                .isEqualTo("problemDetail.s06_error_handling.err04_problem_detail.ProductArchivedException");
    }
}
