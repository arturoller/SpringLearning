package s00_start.solutions.start06_exercises;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprawdzenia z Start06ExercisesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * {@code @WebMvcTest} znajduje konfigurację Start06SolutionsApplication z tego samego pakietu.
 */
@Tag("wzorzec")
@WebMvcTest(SolutionDiscountController.class)
class Start06ExercisesSolutionsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: cena po rabacie")
    void solution1() {
        assertThat(Start06ExercisesSolutions.solution1(10_000, 15)).isEqualTo(8_500);
        assertThat(Start06ExercisesSolutions.solution1(999, 10)).isEqualTo(899);
        assertThat(Start06ExercisesSolutions.solution1(10_000, 0)).isEqualTo(10_000);
    }

    @Test
    @DisplayName("Wzorzec 2: procent spoza 0..100 jest odrzucany")
    void solution2() {
        assertThatThrownBy(() -> Start06ExercisesSolutions.solution2(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rabat musi być w zakresie 0..100: 101");
        assertThatThrownBy(() -> Start06ExercisesSolutions.solution2(-1)).isInstanceOf(IllegalArgumentException.class);
        Start06ExercisesSolutions.solution2(50);
    }

    @Test
    @DisplayName("Wzorzec 3: GET /api/discount zwraca JSON z ceną po rabacie")
    void solution3() throws Exception {
        mockMvc.perform(get("/api/discount").param("price", "10000").param("percent", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discounted").value(8500));
    }

    @Test
    @DisplayName("Wzorzec 3: zły procent → 400 Bad Request")
    void solution3BadRequest() throws Exception {
        mockMvc.perform(get("/api/discount").param("price", "10000").param("percent", "150"))
                .andExpect(status().isBadRequest());
    }
}
