package s06_error_handling.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Err02ExceptionHandler. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Err02ExceptionHandlerExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: czy handler obsłuży wyjątek (ten sam typ lub podklasa)")
    void exercise1() {
        assertThat(Err02ExceptionHandler.canHandle(PaymentException.class, CardDeclinedException.class)).isTrue();
        assertThat(Err02ExceptionHandler.canHandle(PaymentException.class, PaymentException.class)).isTrue();
        assertThat(Err02ExceptionHandler.canHandle(CardDeclinedException.class, PaymentException.class)).isFalse();
        assertThat(Err02ExceptionHandler.canHandle(IllegalArgumentException.class, PaymentException.class)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: odległość w hierarchii klas")
    void exercise2() {
        assertThat(Err02ExceptionHandler.distance(CardDeclinedException.class, CardDeclinedException.class)).isZero();
        assertThat(Err02ExceptionHandler.distance(PaymentException.class, CardDeclinedException.class)).isEqualTo(1);
        assertThat(Err02ExceptionHandler.distance(RuntimeException.class, CardDeclinedException.class)).isEqualTo(2);
        assertThat(Err02ExceptionHandler.distance(IllegalStateException.class, CardDeclinedException.class)).isEqualTo(-1);
    }

    @Test
    @DisplayName("Ćwiczenie 3: wybór handlera o najbliższym typie")
    void exercise3() {
        List<Class<? extends Throwable>> handled = List.of(RuntimeException.class, PaymentException.class, CardDeclinedException.class);

        assertThat(Err02ExceptionHandler.pickHandler(handled, CardDeclinedException.class)).isEqualTo(CardDeclinedException.class);
        assertThat(Err02ExceptionHandler.pickHandler(handled, PaymentException.class)).isEqualTo(PaymentException.class);
        assertThat(Err02ExceptionHandler.pickHandler(handled, IllegalStateException.class)).isEqualTo(RuntimeException.class);
        assertThat(Err02ExceptionHandler.pickHandler(List.of(PaymentException.class), IllegalStateException.class)).isNull();
        assertThatThrownBy(() -> Err02ExceptionHandler.pickHandler(
                List.of(PaymentException.class, PaymentException.class), CardDeclinedException.class))
                .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Ambiguous");
    }
}
