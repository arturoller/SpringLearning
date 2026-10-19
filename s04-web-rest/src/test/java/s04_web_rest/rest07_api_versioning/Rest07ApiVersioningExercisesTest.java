package s04_web_rest.rest07_api_versioning;

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

/**
 * Testy ĆWICZEŃ lekcji Rest07ApiVersioning. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Profil rest07 — to samo wersjonowanie co w aplikacji lekcji (nagłówek API-Version, ?version=, domyślnie 1.0).
 */
@Tag("cwiczenie")
@WebMvcTest(Rest07ExerciseController.class)
@ActiveProfiles("rest07")
class Rest07ApiVersioningExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: wersje 1.x → CustomerV1; nieznany klient → 404")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/cw07/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan Kowalski"));
        mockMvc.perform(get("/api/cw07/customers/4").header("API-Version", "1.5"))
                .andExpect(jsonPath("$.name").value("Zofia Krawczyk"));
        mockMvc.perform(get("/api/cw07/customers/99").header("API-Version", "1.0"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ćwiczenie 2: od 2.0 → CustomerV2 (imię, nazwisko, miasto); 1.5 nadal CustomerV1")
    // Przed rozwiązaniem: 400 — w tym wycinku żadna metoda nie ma wersji 2.0, więc Spring jej nie „wykrył”
    // (detect-supported) i uznaje 2.0 za nieznaną. Po dodaniu mapowania "2.0+" wersja staje się wspierana.
    void exercise2() throws Exception {
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
    @DisplayName("Ćwiczenie 3: porównywanie wersji semantycznych")
    void exercise3() {
        assertThat(Rest07Exercises.compareVersions("1", "1.0.0")).isZero();
        assertThat(Rest07Exercises.compareVersions("1.2", "1.10")).isEqualTo(-1);
        assertThat(Rest07Exercises.compareVersions("v2", "1.9")).isEqualTo(1);
        assertThat(Rest07Exercises.compareVersions("2.0.1", "2.0")).isEqualTo(1);
    }

    @Test
    @DisplayName("Ćwiczenie 4: plan wycofania wersji 1.1 — nagłówki Deprecation, Link i Sunset tylko dla 1.1")
    void exercise4() {
        ApiVersionDeprecationHandler handler = Rest07Exercises.sunsetPlan();
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
