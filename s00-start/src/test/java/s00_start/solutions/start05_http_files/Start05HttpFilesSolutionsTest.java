package s00_start.solutions.start05_http_files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s00_start.start05_http_files.Start05HttpFiles.HttpRequestLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Start05HttpFilesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start05HttpFilesSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: linia zapytania do lokalnej aplikacji")
    void solution1() {
        assertThat(Start05HttpFilesSolutions.solution1("GET", "/api/products"))
                .isEqualTo("GET http://localhost:8080/api/products");
        assertThat(Start05HttpFilesSolutions.solution1("POST", "/api/products"))
                .isEqualTo("POST http://localhost:8080/api/products");
    }

    @Test
    @DisplayName("Wzorzec 2: nazwy kodów statusu")
    void solution2() {
        assertThat(Start05HttpFilesSolutions.solution2(200)).isEqualTo("OK");
        assertThat(Start05HttpFilesSolutions.solution2(201)).isEqualTo("Created");
        assertThat(Start05HttpFilesSolutions.solution2(404)).isEqualTo("Not Found");
        assertThat(Start05HttpFilesSolutions.solution2(415)).isEqualTo("Unsupported Media Type");
        assertThat(Start05HttpFilesSolutions.solution2(418)).isEqualTo("Inny kod: 418");
    }

    @Test
    @DisplayName("Wzorzec 3: rozbiór linii zapytania z pliku .http")
    void solution3() {
        assertThat(Start05HttpFilesSolutions.solution3("  POST   http://localhost:8080/api/products "))
                .isEqualTo(new HttpRequestLine("POST", "http://localhost:8080/api/products"));
        assertThatThrownBy(() -> Start05HttpFilesSolutions.solution3("FETCH http://localhost:8080"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Start05HttpFilesSolutions.solution3("GET localhost:8080"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Start05HttpFilesSolutions.solution3("GET"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
