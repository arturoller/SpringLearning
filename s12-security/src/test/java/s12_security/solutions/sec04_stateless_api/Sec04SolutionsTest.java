package s12_security.solutions.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.cors.CorsConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy rozwiązań wzorcowych ćwiczeń 1–3 lekcji Sec04StatelessApi. @Tag("wzorzec") — biegną w domyślnym ./mvnw test.
 */
@Tag("wzorzec")
class Sec04SolutionsTest {

    private static CorsConfiguration corsFor(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", uri);
        return ExerciseCors.source().getCorsConfiguration(request);
    }

    @Test
    @DisplayName("Wzorzec 1: CSRF wymagany dla POST, PUT, PATCH, DELETE; niewymagany dla GET, HEAD, TRACE, OPTIONS")
    void exercise1() {
        assertThat(CsrfRules.requiresCsrf("POST")).isTrue();
        assertThat(CsrfRules.requiresCsrf("put")).isTrue();
        assertThat(CsrfRules.requiresCsrf("PATCH")).isTrue();
        assertThat(CsrfRules.requiresCsrf("DELETE")).isTrue();
        assertThat(CsrfRules.requiresCsrf("GET")).isFalse();
        assertThat(CsrfRules.requiresCsrf("head")).isFalse();
        assertThat(CsrfRules.requiresCsrf("TRACE")).isFalse();
        assertThat(CsrfRules.requiresCsrf("OPTIONS")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: reguła CORS dla /sec04/cw/** — pochodzenie, metody, nagłówki, maxAge; innych adresów nie dotyczy")
    void exercise2() {
        CorsConfiguration config = corsFor("/sec04/cw/orders");
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:3000");
        assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST");
        assertThat(config.getAllowedHeaders()).containsExactlyInAnyOrder("Authorization", "Content-Type");
        assertThat(config.getMaxAge()).isEqualTo(1800L);
        assertThat(config.checkOrigin("http://zly.example")).isNull();
        assertThat(corsFor("/sec04/api/orders")).isNull();
    }

    @Test
    @DisplayName("Wzorzec 3: punkt wejścia odpowiada 401, WWW-Authenticate i JSON-em problem+json z tytułem \"Zaloguj się\"")
    void exercise3() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExerciseJsonEntryPoint().commence(new MockHttpServletRequest("GET", "/sec04/cw/orders"), response,
                new InsufficientAuthenticationException("brak logowania"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Basic realm=\"cw\"");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        JsonNode body = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("title").asString()).isEqualTo("Zaloguj się");
    }
}
