package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Err01DefaultErrors. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Zwykłe testy jednostkowe. */
@Tag("cwiczenie")
class Err01DefaultErrorsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: domyślne pola ciała błędu")
    void exercise1() {
        assertThat(Err01DefaultErrors.exercise1()).containsExactlyInAnyOrder("timestamp", "status", "error", "path");
    }

    @Test
    @DisplayName("Ćwiczenie 2: bezpieczne właściwości produkcyjne")
    void exercise2() {
        assertThat(Err01DefaultErrors.exercise2())
                .containsEntry("spring.web.error.include-message", "never")
                .containsEntry("spring.web.error.include-stacktrace", "never")
                .containsEntry("spring.web.error.include-binding-errors", "never")
                .containsEntry("spring.web.error.include-exception", "false")
                .hasSize(4);
    }

    @Test
    @DisplayName("Ćwiczenie 3: jednolinijkowy opis błędu z ciała odpowiedzi")
    void exercise3() {
        assertThat(Err01DefaultErrors.exercise3(Map.of("status", 404, "error", "Not Found", "path", "/api/nie-ma")))
                .isEqualTo("404 Not Found: /api/nie-ma");
        assertThat(Err01DefaultErrors.exercise3(
                Map.of("status", 500, "error", "Internal Server Error", "path", "/api/boom", "message", "Brak bazy")))
                .isEqualTo("500 Internal Server Error: /api/boom (Brak bazy)");
    }
}
