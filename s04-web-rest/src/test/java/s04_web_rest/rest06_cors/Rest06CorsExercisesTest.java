package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest06Cors. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Rest06CorsExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: origin = schemat + host (+ jawny port), bez ścieżki i parametrów")
    void exercise1() {
        assertThat(ExerciseCors.originOf("https://sklep.example.com/koszyk?id=1")).isEqualTo("https://sklep.example.com");
        assertThat(ExerciseCors.originOf("http://localhost:3000/app/")).isEqualTo("http://localhost:3000");
    }

    @Test
    @DisplayName("Ćwiczenie 2: inne schemat albo port → inne pochodzenie; inna ścieżka → to samo")
    void exercise2() {
        assertThat(ExerciseCors.sameOrigin("https://sklep.example.com/a", "https://sklep.example.com/b?x=1")).isTrue();
        assertThat(ExerciseCors.sameOrigin("http://sklep.example.com", "https://sklep.example.com")).isFalse();
        assertThat(ExerciseCors.sameOrigin("http://localhost:3000", "http://localhost:8080")).isFalse();
        assertThat(ExerciseCors.sameOrigin("https://sklep.example.com", "https://api.sklep.example.com")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: konfiguracja frontendu — origin, GET/POST, Content-Type, maxAge 1800")
    void exercise3() {
        CorsConfiguration config = ExerciseCors.shopFrontConfig();
        assertThat(config.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(config.checkOrigin("http://localhost:3000")).isNull();
        assertThat(config.checkHttpMethod(HttpMethod.POST)).isNotNull();
        assertThat(config.checkHttpMethod(HttpMethod.DELETE)).isNull();
        assertThat(config.checkHeaders(List.of("Content-Type"))).containsExactly("Content-Type");
        assertThat(config.getMaxAge()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Ćwiczenie 4: wzorzec poddomen + poświadczenia — bez błędu walidacji, nagłówek X-Total-Count ujawniony")
    void exercise4() {
        CorsConfiguration config = ExerciseCors.partnersConfig();
        assertThat(config.checkOrigin("https://abc.partner.example.com")).isEqualTo("https://abc.partner.example.com");
        assertThat(config.checkOrigin("https://evil.example.com")).isNull();
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getExposedHeaders()).containsExactly("X-Total-Count");
        assertThatCode(config::validateAllowCredentials).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ćwiczenie 5: CorsFilter ze źródłem konfiguracji — preflight z dozwolonego pochodzenia 200, z obcego 403")
    void exercise5() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExerciseController())
                .addFilters(new CorsFilter(ExerciseCors.source()))
                .build();

        mockMvc.perform(options("/rest06/cw/orders")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/rest06/cw/orders")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
