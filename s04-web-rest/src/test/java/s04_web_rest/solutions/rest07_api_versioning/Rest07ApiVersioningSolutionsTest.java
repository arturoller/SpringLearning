package s04_web_rest.solutions.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.SemanticApiVersionParser;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprawdzenia z Rest07ApiVersioningExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
@WebMvcTest(Rest07SolutionController.class)
@ActiveProfiles("rest07")
class Rest07ApiVersioningSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: wersje 1.x → CustomerV1; nieznany klient → 404")
    void solution1() throws Exception {
        mockMvc.perform(get("/api/cw07/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan Kowalski"));
        mockMvc.perform(get("/api/cw07/customers/4").header("API-Version", "1.5"))
                .andExpect(jsonPath("$.name").value("Zofia Krawczyk"));
        mockMvc.perform(get("/api/cw07/customers/99").header("API-Version", "1.0"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Wzorzec 2: od 2.0 → CustomerV2 (imię, nazwisko, miasto); 1.5 nadal CustomerV1")
    void solution2() throws Exception {
        mockMvc.perform(get("/api/cw07/customers/1").header("API-Version", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.city").value("Warszawa"));
        mockMvc.perform(get("/api/cw07/customers/4").queryParam("version", "2.0"))
                .andExpect(jsonPath("$.lastName").value("Krawczyk"));
        mockMvc.perform(get("/api/cw07/customers/1").header("API-Version", "1.5"))
                .andExpect(jsonPath("$.name").value("Jan Kowalski"))
                .andExpect(jsonPath("$.firstName").doesNotExist());
    }

    @Test
    @DisplayName("Wzorzec 3: porównywanie wersji semantycznych")
    void solution3() {
        assertThat(Rest07Solutions.compareVersions("1", "1.0.0")).isZero();
        assertThat(Rest07Solutions.compareVersions("1.2", "1.10")).isEqualTo(-1);
        assertThat(Rest07Solutions.compareVersions("v2", "1.9")).isEqualTo(1);
        assertThat(Rest07Solutions.compareVersions("2.0.1", "2.0")).isEqualTo(1);
    }

    @Test
    @DisplayName("Wzorzec 4: plan wycofania wersji 1.1 — nagłówki Deprecation, Link i Sunset tylko dla 1.1")
    void solution4() {
        ApiVersionDeprecationHandler handler = Rest07Solutions.sunsetPlan();
        SemanticApiVersionParser parser = new SemanticApiVersionParser();

        MockHttpServletResponse deprecated = new MockHttpServletResponse();
        handler.handleVersion(parser.parseVersion("1.1"), new Object(), new MockHttpServletRequest(), deprecated);
        long expectedSeconds = ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
        assertThat(deprecated.getHeader("Deprecation")).isEqualTo("@" + expectedSeconds);
        assertThat(deprecated.getHeader("Link")).isEqualTo(
                "<https://sklep.example/api/zmiany-1-1>; rel=\"deprecation\"; type=\"text/html\"");
        assertThat(deprecated.getHeader("Sunset")).isEqualTo("Wed, 30 Jun 2027 00:00:00 GMT");

        MockHttpServletResponse current = new MockHttpServletResponse();
        handler.handleVersion(parser.parseVersion("2.0"), new Object(), new MockHttpServletRequest(), current);
        assertThat(current.getHeaderNames()).isEmpty();
    }
}
