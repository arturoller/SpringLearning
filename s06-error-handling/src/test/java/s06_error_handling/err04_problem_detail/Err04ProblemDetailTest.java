package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 1 lekcji Err04ProblemDetail: sam obiekt ProblemDetail — zwykły test jednostkowy, bez Springa i bez HTTP. */
class Err04ProblemDetailTest {

    @Test
    @DisplayName("1. forStatus: typ nieustawiony (null = about:blank wg RFC), tytuł wynika ze statusu, detail i instance puste")
    void defaults() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getType()).isNull();   // w Spring 7.0.9 brak typu = null; w JSON-ie pola nie ma (RFC: brak = about:blank)
        assertThat(problem.getTitle()).isEqualTo("Not Found");   // getTitle() bez setTitle() zwraca opis statusu
        assertThat(problem.getDetail()).isNull();
        assertThat(problem.getInstance()).isNull();
        assertThat(problem.getProperties()).isNull();
    }

    @Test
    @DisplayName("1. Pięć pól standardu RFC 9457 plus własne pola (properties)")
    void allFields() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Za mało towaru");
        problem.setType(URI.create("https://shop.example.com/problems/out-of-stock"));
        problem.setTitle("Brak towaru");
        problem.setInstance(URI.create("/api/orders/17"));
        problem.setProperty("available", 2);

        assertThat(problem.getType()).hasToString("https://shop.example.com/problems/out-of-stock");
        assertThat(problem.getTitle()).isEqualTo("Brak towaru");
        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Za mało towaru");
        assertThat(problem.getInstance()).hasToString("/api/orders/17");
        assertThat(problem.getProperties()).containsEntry("available", 2);
    }

    @Test
    @DisplayName("4. ErrorResponseException niesie ProblemDetail i status; getBody() zwraca ten sam obiekt")
    void errorResponseException() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Limit");
        ErrorResponseException exception = new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, problem, null);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exception.getBody()).isSameAs(problem);
    }
}
