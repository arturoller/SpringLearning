package s04_web_rest.solutions.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/** Sprawdzenia z Rest07ApiVersioningExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@WebMvcTest(SolutionCustomerVersionedController.class)
class Rest07ApiVersioningSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: pełna postać wersji major.minor.patch")
    void solution1() {
        assertThat(Rest07ApiVersioningSolutions.solution1("2")).isEqualTo("2.0.0");
        assertThat(Rest07ApiVersioningSolutions.solution1("1.1")).isEqualTo("1.1.0");
        assertThat(Rest07ApiVersioningSolutions.solution1("1.2.3")).isEqualTo("1.2.3");
        assertThatThrownBy(() -> Rest07ApiVersioningSolutions.solution1("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rest07ApiVersioningSolutions.solution1("1.x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rest07ApiVersioningSolutions.solution1("1.2.3.4")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: klient w wersji 2.0 (i „2”), wersja 1.0 bez zmian")
    void solution2() throws Exception {
        mockMvc.perform(get("/api/customers/1").header("API-Version", "2.0"))
                .andExpect(content().json("{\"id\":1,\"firstName\":\"Anna\",\"lastName\":\"Nowak\"}", true));
        mockMvc.perform(get("/api/customers/1").header("API-Version", "2"))
                .andExpect(content().json("{\"firstName\":\"Anna\"}"));
        mockMvc.perform(get("/api/customers/1").header("API-Version", "1.0"))
                .andExpect(content().json("{\"id\":1,\"name\":\"Anna Nowak\"}", true));
    }

    @Test
    @DisplayName("Wzorzec 3: symulacja wyboru metody według wersji (stała i baseline)")
    void solution3() {
        List<String> z = List.of("1.0", "1.1+", "2.0");
        assertThat(Rest07ApiVersioningSolutions.solution3(z, "1.0")).isEqualTo("1.0");
        assertThat(Rest07ApiVersioningSolutions.solution3(z, "1.1")).isEqualTo("1.1+");
        assertThat(Rest07ApiVersioningSolutions.solution3(z, "1.5")).isEqualTo("1.1+");
        assertThat(Rest07ApiVersioningSolutions.solution3(z, "2")).isEqualTo("2.0");
        assertThat(Rest07ApiVersioningSolutions.solution3(z, "3.0")).isNull();
        List<String> w = List.of("1.5+", "1.0+");   // kolejność na liście nie ma znaczenia
        assertThat(Rest07ApiVersioningSolutions.solution3(w, "1.1")).isEqualTo("1.0+");
        assertThat(Rest07ApiVersioningSolutions.solution3(w, "3.0")).isEqualTo("1.5+");
        assertThat(Rest07ApiVersioningSolutions.solution3(List.of("1.9", "1.10"), "1.10")).isEqualTo("1.10");
        assertThat(Rest07ApiVersioningSolutions.solution3(List.of("2.0"), "1.0")).isNull();
    }
}
