package s06_error_handling.err04_pitfalls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 4 lekcji Err04ProblemDetail — pułapka: własny {@code @ExceptionHandler} dla wyjątku, który ResponseEntityExceptionHandler
 * już obsługuje. Spring buduje tablicę "wyjątek → metoda" przy starcie aplikacji; tutaj robimy to samo ręcznie, bez uruchamiania kontekstu.
 */
class Err04AmbiguousHandlerTest {

    @Test
    @DisplayName("Sekcja 4 (pułapka): drugi @ExceptionHandler dla tego samego wyjątku w klasie dziedziczącej = błąd (Ambiguous), a nie 'nadpisanie'")
    void duplicateHandlerFailsFast() {
        assertThatThrownBy(() -> new ExceptionHandlerMethodResolver(AmbiguousAdvice.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ambiguous @ExceptionHandler method mapped for")
                .hasMessageContaining("MethodArgumentNotValidException");
    }

    @Test
    @DisplayName("Sekcja 4: właściwy sposób — nadpisać metodę handleMethodArgumentNotValid (jedna metoda, brak duplikatu)")
    void overridingIsFine() {
        assertThat(new ExceptionHandlerMethodResolver(s06_error_handling.err04_problem_detail.Err04Advice.class)
                .resolveMethodByExceptionType(org.springframework.web.bind.MethodArgumentNotValidException.class))
                .isNotNull();
    }
}
