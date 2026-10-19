package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Rest06Cors. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Nierozwiązane ćwiczenie pada z UnsupportedOperationException: TODO (ćwiczenie 1 — także z braku nagłówka CORS).
 */
@Tag("cwiczenie")
@WebMvcTest(Rest06ExerciseController.class)
class Rest06CorsExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: @CrossOrigin — tylko http://localhost:5173 dostaje dane i Access-Control-Allow-Origin")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/cw06/promotions").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(jsonPath("$[0]").value("Kawa -10%"));
        mockMvc.perform(get("/api/cw06/promotions").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ćwiczenie 2: CorsConfiguration sklepu — pochodzenie, metody, nagłówki, credentials, max-age")
    void exercise2() {
        CorsConfiguration config = Rest06Exercises.shopConfiguration();

        assertThat(config.checkOrigin("https://sklep.example")).isEqualTo("https://sklep.example");
        assertThat(config.checkOrigin("https://inny.example")).isNull();
        assertThat(config.checkHttpMethod(HttpMethod.PUT)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.PATCH)).isNull();
        assertThat(config.getExposedHeaders()).containsExactly("Location");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Ćwiczenie 3: subdomeny z credentials — wzorce pochodzeń zamiast \"*\"")
    void exercise3() {
        CorsConfiguration config = Rest06Exercises.subdomainsWithCredentials();

        assertThatCode(config::validateAllowCredentials).doesNotThrowAnyException();
        assertThat(config.checkOrigin("https://admin.sklep.example")).isEqualTo("https://admin.sklep.example");
        assertThat(config.checkOrigin("https://magazyn.sklep.example")).isEqualTo("https://magazyn.sklep.example");
        assertThat(config.checkOrigin("https://sklep.example.evil.com")).isNull();
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.checkHttpMethod(HttpMethod.GET)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.DELETE)).isNull();
    }

    @Test
    @DisplayName("Ćwiczenie 4: kiedy przeglądarka wysyła preflight")
    void exercise4() {
        assertThat(Rest06Exercises.needsPreflight("GET", Map.of())).isFalse();
        assertThat(Rest06Exercises.needsPreflight("POST", Map.of("Content-Type", "text/plain; charset=UTF-8"))).isFalse();
        assertThat(Rest06Exercises.needsPreflight("POST", Map.of("content-type", "application/x-www-form-urlencoded")))
                .isFalse();
        assertThat(Rest06Exercises.needsPreflight("GET", Map.of("Accept-Language", "pl"))).isFalse();
        assertThat(Rest06Exercises.needsPreflight("POST", Map.of("Content-Type", "application/json"))).isTrue();
        assertThat(Rest06Exercises.needsPreflight("DELETE", Map.of())).isTrue();
        assertThat(Rest06Exercises.needsPreflight("PUT", Map.of("Content-Type", "text/plain"))).isTrue();
        assertThat(Rest06Exercises.needsPreflight("GET", Map.of("X-Shop-Id", "WAW-01"))).isTrue();
        assertThat(Rest06Exercises.needsPreflight("GET", Map.of("Authorization", "Bearer abc"))).isTrue();
    }
}
