package s06_error_handling.solutions.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s06_error_handling.err05_custom_exceptions.Err05CustomExceptions.ErrorKind;
import s06_error_handling.err05_custom_exceptions.StorageUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Err05CustomExceptionsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Err05CustomExceptionsSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: kod statusu dla rodzaju błędu")
    void solution1() {
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.MALFORMED_JSON)).isEqualTo(400);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.FIELD_FORMAT)).isEqualTo(400);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.RESOURCE_MISSING_IN_URL)).isEqualTo(404);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.REFERENCE_MISSING_IN_BODY)).isEqualTo(422);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.STATE_CONFLICT)).isEqualTo(409);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.BUSINESS_RULE)).isEqualTo(422);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.DEPENDENCY_DOWN)).isEqualTo(503);
        assertThat(Err05CustomExceptionsSolutions.solution1(ErrorKind.BUG)).isEqualTo(500);
    }

    @Test
    @DisplayName("Wzorzec 2: publiczne ciało błędu nie zdradza komunikatu wewnętrznego przy 5xx")
    void solution2() {
        assertThat(Err05CustomExceptionsSolutions.solution2(404, "Nie ma produktu XXX", "E-1"))
                .containsOnlyKeys("detail").containsEntry("detail", "Nie ma produktu XXX");
        var server = Err05CustomExceptionsSolutions.solution2(500, "Connection refused: jdbc:h2:tcp://db-prod", "E-2");
        assertThat(server).containsOnlyKeys("detail", "errorId").containsEntry("errorId", "E-2");
        assertThat(server.toString()).doesNotContain("jdbc");
        assertThat(Err05CustomExceptionsSolutions.solution2(503, "x", "E-3")).containsEntry("errorId", "E-3");
        assertThatThrownBy(() -> Err05CustomExceptionsSolutions.solution2(200, "x", "E-4")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 3: łańcuch przyczyn jako lista tekstów, odporny na pętlę")
    void solution3() {
        Exception root = new IllegalStateException("Connection refused");
        Exception middle = new RuntimeException(null, root);
        Exception top = new StorageUnavailableException("Raport niedostępny", middle);

        assertThat(Err05CustomExceptionsSolutions.solution3(top)).containsExactly(
                "StorageUnavailableException: Raport niedostępny", "RuntimeException", "IllegalStateException: Connection refused");

        Exception first = new RuntimeException("pierwszy");
        Exception second = new RuntimeException("drugi", first);
        first.initCause(second);
        assertThat(Err05CustomExceptionsSolutions.solution3(first)).containsExactly("RuntimeException: pierwszy", "RuntimeException: drugi");
    }
}
