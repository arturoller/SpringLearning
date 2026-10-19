package s06_error_handling.solutions.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s06_error_handling.err01_default_errors.Err01DefaultErrors.IncludeMode;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Err01DefaultErrorsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Err01DefaultErrorsSolutionsTest {

    private static final Instant NOW = Instant.parse("2026-10-19T07:49:21Z");

    @Test
    @DisplayName("Wzorzec 1: rodzina kodu statusu")
    void solution1() {
        assertThat(Err01DefaultErrorsSolutions.solution1(200)).isEqualTo("sukces");
        assertThat(Err01DefaultErrorsSolutions.solution1(302)).isEqualTo("przekierowanie");
        assertThat(Err01DefaultErrorsSolutions.solution1(404)).isEqualTo("błąd klienta");
        assertThat(Err01DefaultErrorsSolutions.solution1(500)).isEqualTo("błąd serwera");
        assertThat(Err01DefaultErrorsSolutions.solution1(101)).isEqualTo("nieznany");
    }

    @Test
    @DisplayName("Wzorzec 2: domyślne ciało błędu — cztery pola w kolejności Boota")
    void solution2() {
        var body = Err01DefaultErrorsSolutions.solution2(404, "Not Found", "/api/nic", NOW);

        assertThat(body.keySet()).containsExactly("timestamp", "status", "error", "path");
        assertThat(body).containsEntry("timestamp", "2026-10-19T07:49:21Z").containsEntry("status", 404)
                .containsEntry("error", "Not Found").containsEntry("path", "/api/nic");
    }

    @Test
    @DisplayName("Wzorzec 3: pole message zależnie od trybu NEVER / ALWAYS / ON_PARAM")
    void solution3() {
        var never = Err01DefaultErrorsSolutions.solution3(500, "Internal Server Error", "/x", NOW, IncludeMode.NEVER, "true", "tajne");
        assertThat(never.keySet()).containsExactly("timestamp", "status", "error", "path");

        var always = Err01DefaultErrorsSolutions.solution3(500, "Internal Server Error", "/x", NOW, IncludeMode.ALWAYS, null, "tajne");
        assertThat(always.keySet()).containsExactly("timestamp", "status", "error", "message", "path");
        assertThat(always).containsEntry("message", "tajne");

        for (String param : List.of("true", "", "abc", "TRUE")) {
            var onParam = Err01DefaultErrorsSolutions.solution3(500, "Internal Server Error", "/x", NOW, IncludeMode.ON_PARAM, param, "tajne");
            assertThat(onParam).as("parametr message=" + param).containsKey("message");
        }
        for (String param : new String[]{null, "false", "FALSE"}) {
            var onParam = Err01DefaultErrorsSolutions.solution3(500, "Internal Server Error", "/x", NOW, IncludeMode.ON_PARAM, param, "tajne");
            assertThat(onParam).as("parametr message=" + param).doesNotContainKey("message");
        }
    }
}
