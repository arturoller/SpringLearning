package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s06_error_handling.err05_custom_exceptions.Err05CustomExceptions.ErrorKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Err05CustomExceptions. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Err05CustomExceptionsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: kod statusu dla rodzaju błędu")
    void exercise1() {
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.MALFORMED_JSON)).isEqualTo(400);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.FIELD_FORMAT)).isEqualTo(400);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.RESOURCE_MISSING_IN_URL)).isEqualTo(404);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.REFERENCE_MISSING_IN_BODY)).isEqualTo(422);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.STATE_CONFLICT)).isEqualTo(409);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.BUSINESS_RULE)).isEqualTo(422);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.DEPENDENCY_DOWN)).isEqualTo(503);
        assertThat(Err05CustomExceptions.statusFor(ErrorKind.BUG)).isEqualTo(500);
    }

    @Test
    @DisplayName("Ćwiczenie 2: publiczne ciało błędu nie zdradza komunikatu wewnętrznego przy 5xx")
    void exercise2() {
        assertThat(Err05CustomExceptions.publicBody(404, "Nie ma produktu XXX", "E-1"))
                .containsOnlyKeys("detail").containsEntry("detail", "Nie ma produktu XXX");
        var server = Err05CustomExceptions.publicBody(500, "Connection refused: jdbc:h2:tcp://db-prod", "E-2");
        assertThat(server).containsOnlyKeys("detail", "errorId").containsEntry("errorId", "E-2");
        assertThat(server.toString()).doesNotContain("jdbc");
        assertThat(Err05CustomExceptions.publicBody(503, "x", "E-3")).containsEntry("errorId", "E-3");
        assertThatThrownBy(() -> Err05CustomExceptions.publicBody(200, "x", "E-4")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: łańcuch przyczyn jako lista tekstów, odporny na pętlę")
    void exercise3() {
        Exception root = new IllegalStateException("Connection refused");
        Exception middle = new RuntimeException(null, root);   // komunikat null → sama nazwa klasy
        Exception top = new StorageUnavailableException("Raport niedostępny", middle);

        assertThat(Err05CustomExceptions.causeChain(top)).containsExactly(
                "StorageUnavailableException: Raport niedostępny", "RuntimeException", "IllegalStateException: Connection refused");

        Exception first = new RuntimeException("pierwszy");
        Exception second = new RuntimeException("drugi", first);
        first.initCause(second);   // pętla: first → second → first
        assertThat(Err05CustomExceptions.causeChain(first)).containsExactly("RuntimeException: pierwszy", "RuntimeException: drugi");
    }
}
