package s06_error_handling.solutions.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s06_error_handling.err02_exception_handler.CardDeclinedException;
import s06_error_handling.err02_exception_handler.PaymentException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Err02ExceptionHandlerExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Err02ExceptionHandlerSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: czy handler obsłuży wyjątek (ten sam typ lub podklasa)")
    void solution1() {
        assertThat(Err02ExceptionHandlerSolutions.solution1(PaymentException.class, CardDeclinedException.class)).isTrue();
        assertThat(Err02ExceptionHandlerSolutions.solution1(PaymentException.class, PaymentException.class)).isTrue();
        assertThat(Err02ExceptionHandlerSolutions.solution1(CardDeclinedException.class, PaymentException.class)).isFalse();
        assertThat(Err02ExceptionHandlerSolutions.solution1(IllegalArgumentException.class, PaymentException.class)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: odległość w hierarchii klas")
    void solution2() {
        assertThat(Err02ExceptionHandlerSolutions.solution2(CardDeclinedException.class, CardDeclinedException.class)).isZero();
        assertThat(Err02ExceptionHandlerSolutions.solution2(PaymentException.class, CardDeclinedException.class)).isEqualTo(1);
        assertThat(Err02ExceptionHandlerSolutions.solution2(RuntimeException.class, CardDeclinedException.class)).isEqualTo(2);
        assertThat(Err02ExceptionHandlerSolutions.solution2(IllegalStateException.class, CardDeclinedException.class)).isEqualTo(-1);
    }

    @Test
    @DisplayName("Wzorzec 3: wybór handlera o najbliższym typie")
    void solution3() {
        List<Class<? extends Throwable>> handled = List.of(RuntimeException.class, PaymentException.class, CardDeclinedException.class);

        assertThat(Err02ExceptionHandlerSolutions.solution3(handled, CardDeclinedException.class)).isEqualTo(CardDeclinedException.class);
        assertThat(Err02ExceptionHandlerSolutions.solution3(handled, PaymentException.class)).isEqualTo(PaymentException.class);
        assertThat(Err02ExceptionHandlerSolutions.solution3(handled, IllegalStateException.class)).isEqualTo(RuntimeException.class);
        assertThat(Err02ExceptionHandlerSolutions.solution3(List.of(PaymentException.class), IllegalStateException.class)).isNull();
        assertThatThrownBy(() -> Err02ExceptionHandlerSolutions.solution3(
                List.of(PaymentException.class, PaymentException.class), CardDeclinedException.class))
                .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("Ambiguous");
    }
}
