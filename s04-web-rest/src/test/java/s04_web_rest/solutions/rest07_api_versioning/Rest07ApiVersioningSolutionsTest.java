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
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ROZWIĄZAŃ WZORCOWYCH lekcji Rest07ApiVersioning. @Tag("wzorzec") — biegną domyślnie w ./mvnw test (dowód, że ćwiczenia są rozwiązywalne). */
@Tag("wzorzec")
@WebMvcTest(Rest07SolutionsController.class)
@ActiveProfiles("rest07") // profil lekcji: wersja z nagłówka API-Version
class Rest07ApiVersioningSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: wersja 1.0 → fullName")
    void exercise1() throws Exception {
        mockMvc.perform(get("/rest07/cw/customers/7").header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.fullName").value("Jan Kowalski"));
    }

    @Test
    @DisplayName("Wzorzec 2: wersja 2.0 → firstName i lastName, bez fullName")
    void exercise2() throws Exception {
        mockMvc.perform(get("/rest07/cw/customers/7").header("API-Version", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.fullName").doesNotExist());
    }

    @Test
    @DisplayName("Wzorzec 3: „1.1+” → zamówienia w 1.1 i 2.0, a w 1.0 brak endpointu (404)")
    void exercise3() throws Exception {
        mockMvc.perform(get("/rest07/cw/customers/7/orders").header("API-Version", "1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Z-7-1"))
                .andExpect(jsonPath("$[1]").value("Z-7-2"));
        mockMvc.perform(get("/rest07/cw/customers/7/orders").header("API-Version", "2.0"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest07/cw/customers/7/orders").header("API-Version", "1.0"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Wzorzec 4: porównanie wersji jak w Springu — 1.10 > 1.9, 2 = 2.0.0, v2 > 1.5")
    void exercise4() {
        assertThat(Rest07SolutionsVersions.isNewer("1.10", "1.9")).isTrue();
        assertThat(Rest07SolutionsVersions.isNewer("1.9", "1.10")).isFalse();
        assertThat(Rest07SolutionsVersions.isNewer("2", "2.0.0")).isFalse();
        assertThat(Rest07SolutionsVersions.isNewer("v2", "1.5")).isTrue();
    }

    @Test
    @DisplayName("Wzorzec 5: obsługa wycofania 1.1 dopisuje Deprecation, Sunset i Link; dla 2.0 — nic")
    void exercise5() {
        StandardApiVersionDeprecationHandler handler = Rest07SolutionsVersions.deprecationOf1dot1();
        SemanticApiVersionParser parser = new SemanticApiVersionParser();

        MockHttpServletResponse deprecated = new MockHttpServletResponse();
        handler.handleVersion(parser.parseVersion("1.1"), new Object(), new MockHttpServletRequest(), deprecated);
        assertThat(deprecated.getHeader("Deprecation")).isEqualTo("@1780272000");   // 2026-06-01T00:00Z
        assertThat(deprecated.getHeader("Sunset")).isEqualTo("Wed, 30 Jun 2027 00:00:00 GMT");
        assertThat(deprecated.getHeader("Link")).startsWith("<https://sklep.example.com/docs/api/v2>; rel=\"sunset\"");

        MockHttpServletResponse current = new MockHttpServletResponse();
        handler.handleVersion(parser.parseVersion("2.0"), new Object(), new MockHttpServletRequest(), current);
        assertThat(current.getHeaderNames()).isEmpty();
    }
}
