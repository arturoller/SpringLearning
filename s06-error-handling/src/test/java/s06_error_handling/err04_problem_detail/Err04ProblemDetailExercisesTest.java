package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Err04ProblemDetail. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Err04ProblemDetailExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: czy to typ treści „problem”")
    void exercise1() {
        assertThat(Err04ProblemDetail.isProblemMediaType("application/problem+json")).isTrue();
        assertThat(Err04ProblemDetail.isProblemMediaType("Application/Problem+JSON;charset=UTF-8")).isTrue();
        assertThat(Err04ProblemDetail.isProblemMediaType("application/problem+xml")).isTrue();
        assertThat(Err04ProblemDetail.isProblemMediaType("application/json")).isFalse();
        assertThat(Err04ProblemDetail.isProblemMediaType("text/plain")).isFalse();
        assertThat(Err04ProblemDetail.isProblemMediaType(null)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: ciało problemu — kolejność, pomijanie null, pola własne bez nadpisywania standardowych")
    void exercise2() {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("sku", "ELE-001");
        extensions.put("title", "zly");

        Map<String, Object> body = Err04ProblemDetail.problemBody(404, "Brak", null, "/x", extensions);

        assertThat(body.keySet()).containsExactly("status", "title", "instance", "sku");
        assertThat(body).containsEntry("status", 404).containsEntry("title", "Brak")
                .containsEntry("instance", "/x").containsEntry("sku", "ELE-001");
        assertThat(Err04ProblemDetail.problemBody(500, null, null, null, Map.of()).keySet()).containsExactly("status");
    }

    @Test
    @DisplayName("Ćwiczenie 3: wyszukiwanie komunikatu z języka, potem z pliku domyślnego, z argumentami")
    void exercise3() {
        Map<String, Map<String, String>> bundles = Map.of(
                "", Map.of("metoda", "Metoda {0} niedozwolona. Dozwolone: {1}.", "tylko.pl", "Tylko po polsku"),
                "en", Map.of("metoda", "Method {0} not allowed. Allowed: {1}."));

        assertThat(Err04ProblemDetail.resolveMessage(bundles, "en", "metoda", "DELETE", "[GET]"))
                .isEqualTo("Method DELETE not allowed. Allowed: [GET].");
        assertThat(Err04ProblemDetail.resolveMessage(bundles, "de", "metoda", "DELETE", "[GET]"))
                .isEqualTo("Metoda DELETE niedozwolona. Dozwolone: [GET].");
        assertThat(Err04ProblemDetail.resolveMessage(bundles, "en", "tylko.pl")).isEqualTo("Tylko po polsku");
        assertThat(Err04ProblemDetail.resolveMessage(bundles, "en", "nie.ma")).isNull();
    }
}
