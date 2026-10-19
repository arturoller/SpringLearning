package s15_actuator_logging.obs04_logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Obs04Logging. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Obs04LoggingExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: czy komunikat danego poziomu zostanie wypisany")
    void exercise1() {
        assertThat(Obs04Logging.exercise1("INFO", "debug")).isFalse();
        assertThat(Obs04Logging.exercise1("info", "ERROR")).isTrue();
        assertThat(Obs04Logging.exercise1("DEBUG", "DEBUG")).isTrue();
        assertThat(Obs04Logging.exercise1("WARN", "info")).isFalse();
        assertThat(Obs04Logging.exercise1("TRACE", "trace")).isTrue();
        assertThatThrownBy(() -> Obs04Logging.exercise1("LOUD", "INFO")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Obs04Logging.exercise1("INFO", "LOUD")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: maskowanie numeru karty")
    void exercise2() {
        assertThat(Obs04Logging.exercise2("4111 1111 1111 1234")).isEqualTo("**** **** **** 1234");
        assertThat(Obs04Logging.exercise2("4111-1111-1111-1234")).isEqualTo("****-****-****-1234");
        assertThat(Obs04Logging.exercise2("12")).isEqualTo("**");
        assertThat(Obs04Logging.exercise2("1234")).isEqualTo("****");
        assertThat(Obs04Logging.exercise2("12345")).isEqualTo("*2345");
        assertThat(Obs04Logging.exercise2(null)).isEqualTo("brak");
    }

    @Test
    @DisplayName("Ćwiczenie 3: poziom efektywny rejestratora (dziedziczenie po kropkach)")
    void exercise3() {
        Map<String, String> configured = Map.of("ROOT", "WARN", "app.shop", "DEBUG", "app.shop.pay", "ERROR");

        assertThat(Obs04Logging.exercise3(configured, "app.shop.pay.Charge")).isEqualTo("ERROR");
        assertThat(Obs04Logging.exercise3(configured, "app.shop.pay")).isEqualTo("ERROR");
        assertThat(Obs04Logging.exercise3(configured, "app.shop.Order")).isEqualTo("DEBUG");
        assertThat(Obs04Logging.exercise3(configured, "app.Other")).isEqualTo("WARN");
        assertThat(Obs04Logging.exercise3(configured, "lib.X")).isEqualTo("WARN");
        assertThat(Obs04Logging.exercise3(Map.of(), "app.Other")).isEqualTo("INFO");
    }
}
