package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s06_error_handling.err01_default_errors.Err01DefaultErrors.IncludeMode;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Err01DefaultErrors. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Err01DefaultErrorsExercisesTest {

    private static final Instant NOW = Instant.parse("2026-10-19T07:49:21Z");

    @Test
    @DisplayName("Ćwiczenie 1: rodzina kodu statusu")
    void exercise1() {
        assertThat(Err01DefaultErrors.statusFamily(200)).isEqualTo("sukces");
        assertThat(Err01DefaultErrors.statusFamily(302)).isEqualTo("przekierowanie");
        assertThat(Err01DefaultErrors.statusFamily(404)).isEqualTo("błąd klienta");
        assertThat(Err01DefaultErrors.statusFamily(500)).isEqualTo("błąd serwera");
        assertThat(Err01DefaultErrors.statusFamily(101)).isEqualTo("nieznany");
    }

    @Test
    @DisplayName("Ćwiczenie 2: domyślne ciało błędu — cztery pola w kolejności Boota")
    void exercise2() {
        var body = Err01DefaultErrors.defaultErrorBody(404, "Not Found", "/api/nic", NOW);

        assertThat(body.keySet()).containsExactly("timestamp", "status", "error", "path");
        assertThat(body).containsEntry("timestamp", "2026-10-19T07:49:21Z").containsEntry("status", 404)
                .containsEntry("error", "Not Found").containsEntry("path", "/api/nic");
    }

    @Test
    @DisplayName("Ćwiczenie 3: pole message zależnie od trybu NEVER / ALWAYS / ON_PARAM")
    void exercise3() {
        var never = Err01DefaultErrors.errorBody(500, "Internal Server Error", "/x", NOW, IncludeMode.NEVER, "true", "tajne");
        assertThat(never.keySet()).containsExactly("timestamp", "status", "error", "path");

        var always = Err01DefaultErrors.errorBody(500, "Internal Server Error", "/x", NOW, IncludeMode.ALWAYS, null, "tajne");
        assertThat(always.keySet()).containsExactly("timestamp", "status", "error", "message", "path");
        assertThat(always).containsEntry("message", "tajne");

        for (String param : List.of("true", "", "abc", "TRUE")) {
            var onParam = Err01DefaultErrors.errorBody(500, "Internal Server Error", "/x", NOW, IncludeMode.ON_PARAM, param, "tajne");
            assertThat(onParam).as("parametr message=" + param).containsKey("message");
        }
        for (String param : new String[]{null, "false", "FALSE"}) {
            var onParam = Err01DefaultErrors.errorBody(500, "Internal Server Error", "/x", NOW, IncludeMode.ON_PARAM, param, "tajne");
            assertThat(onParam).as("parametr message=" + param).doesNotContainKey("message");
        }
    }
}
