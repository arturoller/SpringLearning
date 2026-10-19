package s04_web_rest.solutions.rest06_cors;

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

/** Sprawdzenia z Rest06CorsExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
@WebMvcTest(Rest06SolutionController.class)
class Rest06CorsSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: @CrossOrigin — tylko http://localhost:5173 dostaje dane i Access-Control-Allow-Origin")
    void solution1() throws Exception {
        mockMvc.perform(get("/api/cw06/promotions").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(jsonPath("$[0]").value("Kawa -10%"));
        mockMvc.perform(get("/api/cw06/promotions").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Wzorzec 2: CorsConfiguration sklepu — pochodzenie, metody, nagłówki, credentials, max-age")
    void solution2() {
        CorsConfiguration config = Rest06Solutions.shopConfiguration();

        assertThat(config.checkOrigin("https://sklep.example")).isEqualTo("https://sklep.example");
        assertThat(config.checkOrigin("https://inny.example")).isNull();
        assertThat(config.checkHttpMethod(HttpMethod.PUT)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.PATCH)).isNull();
        assertThat(config.getExposedHeaders()).containsExactly("Location");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Wzorzec 3: subdomeny z credentials — wzorce pochodzeń zamiast \"*\"")
    void solution3() {
        CorsConfiguration config = Rest06Solutions.subdomainsWithCredentials();

        assertThatCode(config::validateAllowCredentials).doesNotThrowAnyException();
        assertThat(config.checkOrigin("https://admin.sklep.example")).isEqualTo("https://admin.sklep.example");
        assertThat(config.checkOrigin("https://magazyn.sklep.example")).isEqualTo("https://magazyn.sklep.example");
        assertThat(config.checkOrigin("https://sklep.example.evil.com")).isNull();
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.checkHttpMethod(HttpMethod.GET)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.DELETE)).isNull();
    }

    @Test
    @DisplayName("Wzorzec 4: kiedy przeglądarka wysyła preflight")
    void solution4() {
        assertThat(Rest06Solutions.needsPreflight("GET", Map.of())).isFalse();
        assertThat(Rest06Solutions.needsPreflight("POST", Map.of("Content-Type", "text/plain; charset=UTF-8"))).isFalse();
        assertThat(Rest06Solutions.needsPreflight("POST", Map.of("content-type", "application/x-www-form-urlencoded")))
                .isFalse();
        assertThat(Rest06Solutions.needsPreflight("GET", Map.of("Accept-Language", "pl"))).isFalse();
        assertThat(Rest06Solutions.needsPreflight("POST", Map.of("Content-Type", "application/json"))).isTrue();
        assertThat(Rest06Solutions.needsPreflight("DELETE", Map.of())).isTrue();
        assertThat(Rest06Solutions.needsPreflight("PUT", Map.of("Content-Type", "text/plain"))).isTrue();
        assertThat(Rest06Solutions.needsPreflight("GET", Map.of("X-Shop-Id", "WAW-01"))).isTrue();
        assertThat(Rest06Solutions.needsPreflight("GET", Map.of("Authorization", "Bearer abc"))).isTrue();
    }
}
