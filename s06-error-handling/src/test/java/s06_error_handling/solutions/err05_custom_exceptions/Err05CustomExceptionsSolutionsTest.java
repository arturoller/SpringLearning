package s06_error_handling.solutions.err05_custom_exceptions;

import s06_error_handling.err05_custom_exceptions.ErrorCode;
import s06_error_handling.err05_custom_exceptions.OutOfStockException;
import s06_error_handling.err05_custom_exceptions.ProductNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Err05CustomExceptions. @Tag("wzorzec") — uruchamiasz sam (▶ obok klasy). Zwykłe testy jednostkowe. */
@Tag("wzorzec")
class Err05CustomExceptionsSolutionsTest {

    @Test
    @DisplayName("Ćwiczenie 1: kod błędu domenowego → status HTTP")
    void exercise1() {
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorCode.PRODUCT_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorCode.OUT_OF_STOCK)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorCode.PAYMENT_UNAVAILABLE)).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Ćwiczenie 2: grupowanie błędów walidacji po polach (posortowane)")
    void exercise2() {
        var result = Err05CustomExceptionsSolutions.solution2(List.of(
                new Err05CustomExceptionsSolutions.FieldMessage("quantity", "Minimalna ilość to 1"),
                new Err05CustomExceptionsSolutions.FieldMessage("customer", "Podaj nazwę klienta"),
                new Err05CustomExceptionsSolutions.FieldMessage("quantity", "Ilość musi być liczbą")));

        assertThat(result.keySet()).containsExactly("customer", "quantity");
        assertThat(result.get("quantity")).containsExactly("Ilość musi być liczbą", "Minimalna ilość to 1");
        assertThat(Err05CustomExceptionsSolutions.solution2(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: ProblemDetail z wyjątku sklepu (status, detail, code, pola OutOfStock)")
    void exercise3() {
        ProblemDetail notFound = Err05CustomExceptionsSolutions.solution3(new ProductNotFoundException(5));
        assertThat(notFound.getStatus()).isEqualTo(404);
        assertThat(notFound.getDetail()).isEqualTo("Nie znaleziono produktu o id 5");
        assertThat(notFound.getProperties()).containsEntry("code", "PRODUCT_NOT_FOUND").doesNotContainKey("requested");

        ProblemDetail stock = Err05CustomExceptionsSolutions.solution3(new OutOfStockException(2, 3, 0));
        assertThat(stock.getStatus()).isEqualTo(409);
        assertThat(stock.getProperties()).containsEntry("code", "OUT_OF_STOCK")
                .containsEntry("requested", 3).containsEntry("available", 0);
    }
}
