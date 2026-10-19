package s15_actuator_logging.solutions.obs04_logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Obs04LoggingExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Obs04LoggingSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: czy komunikat danego poziomu zostanie wypisany")
    void solution1() {
        assertThat(Obs04LoggingSolutions.solution1("INFO", "debug")).isFalse();
        assertThat(Obs04LoggingSolutions.solution1("info", "ERROR")).isTrue();
        assertThat(Obs04LoggingSolutions.solution1("DEBUG", "DEBUG")).isTrue();
        assertThat(Obs04LoggingSolutions.solution1("WARN", "info")).isFalse();
        assertThat(Obs04LoggingSolutions.solution1("TRACE", "trace")).isTrue();
        assertThatThrownBy(() -> Obs04LoggingSolutions.solution1("LOUD", "INFO"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Obs04LoggingSolutions.solution1("INFO", "LOUD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: maskowanie numeru karty")
    void solution2() {
        assertThat(Obs04LoggingSolutions.solution2("4111 1111 1111 1234")).isEqualTo("**** **** **** 1234");
        assertThat(Obs04LoggingSolutions.solution2("4111-1111-1111-1234")).isEqualTo("****-****-****-1234");
        assertThat(Obs04LoggingSolutions.solution2("12")).isEqualTo("**");
        assertThat(Obs04LoggingSolutions.solution2("1234")).isEqualTo("****");
        assertThat(Obs04LoggingSolutions.solution2("12345")).isEqualTo("*2345");
        assertThat(Obs04LoggingSolutions.solution2(null)).isEqualTo("brak");
    }

    @Test
    @DisplayName("Wzorzec 3: poziom efektywny rejestratora (dziedziczenie po kropkach)")
    void solution3() {
        Map<String, String> configured = Map.of("ROOT", "WARN", "app.shop", "DEBUG", "app.shop.pay", "ERROR");

        assertThat(Obs04LoggingSolutions.solution3(configured, "app.shop.pay.Charge")).isEqualTo("ERROR");
        assertThat(Obs04LoggingSolutions.solution3(configured, "app.shop.pay")).isEqualTo("ERROR");
        assertThat(Obs04LoggingSolutions.solution3(configured, "app.shop.Order")).isEqualTo("DEBUG");
        assertThat(Obs04LoggingSolutions.solution3(configured, "app.Other")).isEqualTo("WARN");
        assertThat(Obs04LoggingSolutions.solution3(configured, "lib.X")).isEqualTo("WARN");
        assertThat(Obs04LoggingSolutions.solution3(Map.of(), "app.Other")).isEqualTo("INFO");
    }
}
