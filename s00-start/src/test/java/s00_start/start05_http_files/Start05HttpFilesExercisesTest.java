package s00_start.start05_http_files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s00_start.start05_http_files.Start05HttpFiles.HttpRequestLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Start05HttpFiles. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Start05HttpFilesExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: linia zapytania do lokalnej aplikacji")
    void exercise1() {
        assertThat(Start05HttpFiles.exercise1("GET", "/api/products")).isEqualTo("GET http://localhost:8080/api/products");
        assertThat(Start05HttpFiles.exercise1("POST", "/api/products")).isEqualTo("POST http://localhost:8080/api/products");
    }

    @Test
    @DisplayName("Ćwiczenie 2: nazwy kodów statusu")
    void exercise2() {
        assertThat(Start05HttpFiles.exercise2(200)).isEqualTo("OK");
        assertThat(Start05HttpFiles.exercise2(201)).isEqualTo("Created");
        assertThat(Start05HttpFiles.exercise2(404)).isEqualTo("Not Found");
        assertThat(Start05HttpFiles.exercise2(415)).isEqualTo("Unsupported Media Type");
        assertThat(Start05HttpFiles.exercise2(418)).isEqualTo("Inny kod: 418");
    }

    @Test
    @DisplayName("Ćwiczenie 3: rozbiór linii zapytania z pliku .http")
    void exercise3() {
        assertThat(Start05HttpFiles.exercise3("  POST   http://localhost:8080/api/products "))
                .isEqualTo(new HttpRequestLine("POST", "http://localhost:8080/api/products"));
        assertThatThrownBy(() -> Start05HttpFiles.exercise3("FETCH http://localhost:8080"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Start05HttpFiles.exercise3("GET localhost:8080"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Start05HttpFiles.exercise3("GET"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
