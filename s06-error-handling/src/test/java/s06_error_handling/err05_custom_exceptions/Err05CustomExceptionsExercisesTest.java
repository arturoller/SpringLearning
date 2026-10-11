package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Err05CustomExceptions. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Zwykłe testy jednostkowe. */
@Tag("cwiczenie")
class Err05CustomExceptionsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: kod błędu domenowego → status HTTP")
    void exercise1() {
        assertThat(Err05CustomExceptions.exercise1(ErrorCode.PRODUCT_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Err05CustomExceptions.exercise1(ErrorCode.OUT_OF_STOCK)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(Err05CustomExceptions.exercise1(ErrorCode.PAYMENT_UNAVAILABLE)).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Ćwiczenie 2: grupowanie błędów walidacji po polach (posortowane)")
    void exercise2() {
        var result = Err05CustomExceptions.exercise2(List.of(
                new Err05CustomExceptions.FieldMessage("quantity", "Minimalna ilość to 1"),
                new Err05CustomExceptions.FieldMessage("customer", "Podaj nazwę klienta"),
                new Err05CustomExceptions.FieldMessage("quantity", "Ilość musi być liczbą")));

        assertThat(result.keySet()).containsExactly("customer", "quantity");
        assertThat(result.get("quantity")).containsExactly("Ilość musi być liczbą", "Minimalna ilość to 1");
        assertThat(Err05CustomExceptions.exercise2(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: ProblemDetail z wyjątku sklepu (status, detail, code, pola OutOfStock)")
    void exercise3() {
        ProblemDetail notFound = Err05CustomExceptions.exercise3(new ProductNotFoundException(5));
        assertThat(notFound.getStatus()).isEqualTo(404);
        assertThat(notFound.getDetail()).isEqualTo("Nie znaleziono produktu o id 5");
        assertThat(notFound.getProperties()).containsEntry("code", "PRODUCT_NOT_FOUND").doesNotContainKey("requested");

        ProblemDetail stock = Err05CustomExceptions.exercise3(new OutOfStockException(2, 3, 0));
        assertThat(stock.getStatus()).isEqualTo(409);
        assertThat(stock.getProperties()).containsEntry("code", "OUT_OF_STOCK")
                .containsEntry("requested", 3).containsEntry("available", 0);
    }
}
