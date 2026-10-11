package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Rest06Cors. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * ExerciseCorsConfig nie ma {@code @Configuration}, więc dokładamy ją przez {@code @Import}. Dopóki szkielet rzuca
 * wyjątek, kontekst w ogóle się nie uruchomi — wszystkie testy tej klasy będą czerwone.
 */
@Tag("cwiczenie")
@WebMvcTest(ReportsController.class)
@Import(ExerciseCorsConfig.class)
class Rest06CorsExercisesTest {

    static final String REPORTS = "https://raporty.example.com";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: pochodzenie z pełnego adresu")
    void exercise1() {
        assertThat(Rest06Cors.exercise1("https://sklep.example.com:8443/api/x?y=1")).isEqualTo("https://sklep.example.com:8443");
        assertThat(Rest06Cors.exercise1("http://localhost/api")).isEqualTo("http://localhost");
    }

    @Test
    @DisplayName("Ćwiczenie 2: to samo pochodzenie — schemat, host i port (z portami domyślnymi)")
    void exercise2() {
        assertThat(Rest06Cors.exercise2("https://sklep.example.com/a", "https://sklep.example.com:443/b")).isTrue();
        assertThat(Rest06Cors.exercise2("HTTP://Localhost/a", "http://localhost:80")).isTrue();
        assertThat(Rest06Cors.exercise2("http://localhost:3000", "http://localhost:8080")).isFalse();
        assertThat(Rest06Cors.exercise2("https://a.example.com", "https://b.example.com")).isFalse();
        assertThat(Rest06Cors.exercise2("http://sklep.example.com", "https://sklep.example.com")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: CORS dla /api/exercise/** — GET i PUT z raportów, max-age 600, reszta 403")
    void exercise3() throws Exception {
        mockMvc.perform(options("/api/exercise/reports/1").header(HttpHeaders.ORIGIN, REPORTS)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REPORTS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "600"));
        mockMvc.perform(get("/api/exercise/reports/1").header(HttpHeaders.ORIGIN, REPORTS))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REPORTS));
        mockMvc.perform(options("/api/exercise/reports/1").header(HttpHeaders.ORIGIN, REPORTS)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exercise/reports/1").header(HttpHeaders.ORIGIN, "https://sklep.example.com"))
                .andExpect(status().isForbidden());
    }
}
