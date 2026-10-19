package s06_error_handling.solutions.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Err04ProblemDetailExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Err04ProblemDetailSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: czy to typ treści „problem”")
    void solution1() {
        assertThat(Err04ProblemDetailSolutions.solution1("application/problem+json")).isTrue();
        assertThat(Err04ProblemDetailSolutions.solution1("Application/Problem+JSON;charset=UTF-8")).isTrue();
        assertThat(Err04ProblemDetailSolutions.solution1("application/problem+xml")).isTrue();
        assertThat(Err04ProblemDetailSolutions.solution1("application/json")).isFalse();
        assertThat(Err04ProblemDetailSolutions.solution1("text/plain")).isFalse();
        assertThat(Err04ProblemDetailSolutions.solution1(null)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: ciało problemu — kolejność, pomijanie null, pola własne bez nadpisywania standardowych")
    void solution2() {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("sku", "ELE-001");
        extensions.put("title", "zly");

        Map<String, Object> body = Err04ProblemDetailSolutions.solution2(404, "Brak", null, "/x", extensions);

        assertThat(body.keySet()).containsExactly("status", "title", "instance", "sku");
        assertThat(body).containsEntry("status", 404).containsEntry("title", "Brak")
                .containsEntry("instance", "/x").containsEntry("sku", "ELE-001");
        assertThat(Err04ProblemDetailSolutions.solution2(500, null, null, null, Map.of()).keySet()).containsExactly("status");
    }

    @Test
    @DisplayName("Wzorzec 3: wyszukiwanie komunikatu z języka, potem z pliku domyślnego, z argumentami")
    void solution3() {
        Map<String, Map<String, String>> bundles = Map.of(
                "", Map.of("metoda", "Metoda {0} niedozwolona. Dozwolone: {1}.", "tylko.pl", "Tylko po polsku"),
                "en", Map.of("metoda", "Method {0} not allowed. Allowed: {1}."));

        assertThat(Err04ProblemDetailSolutions.solution3(bundles, "en", "metoda", "DELETE", "[GET]"))
                .isEqualTo("Method DELETE not allowed. Allowed: [GET].");
        assertThat(Err04ProblemDetailSolutions.solution3(bundles, "de", "metoda", "DELETE", "[GET]"))
                .isEqualTo("Metoda DELETE niedozwolona. Dozwolone: [GET].");
        assertThat(Err04ProblemDetailSolutions.solution3(bundles, "en", "tylko.pl")).isEqualTo("Tylko po polsku");
        assertThat(Err04ProblemDetailSolutions.solution3(bundles, "en", "nie.ma")).isNull();
    }
}
