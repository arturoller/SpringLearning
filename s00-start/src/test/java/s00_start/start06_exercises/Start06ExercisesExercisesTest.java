package s00_start.start06_exercises;

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
 * Testy ĆWICZEŃ lekcji Start06Exercises. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * To @WebMvcTest (ćwiczenie 3 dotyczy kontrolera), ale metody exercise1/exercise2 sprawdzamy zwyczajnie, bez MockMvc.
 */
@Tag("cwiczenie")
@WebMvcTest(DiscountController.class)
class Start06ExercisesExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: cena po rabacie")
    void exercise1() {
        assertThat(Start06Exercises.exercise1(10_000, 15)).isEqualTo(8_500);
        assertThat(Start06Exercises.exercise1(999, 10)).isEqualTo(899);
        assertThat(Start06Exercises.exercise1(10_000, 0)).isEqualTo(10_000);
    }

    @Test
    @DisplayName("Ćwiczenie 2: procent spoza 0..100 jest odrzucany")
    void exercise2() {
        assertThatThrownBy(() -> Start06Exercises.exercise2(101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rabat musi być w zakresie 0..100: 101");
        assertThatThrownBy(() -> Start06Exercises.exercise2(-1)).isInstanceOf(IllegalArgumentException.class);
        Start06Exercises.exercise2(50);   // poprawny procent — brak wyjątku
    }

    @Test
    @DisplayName("Ćwiczenie 3: GET /api/discount zwraca JSON z ceną po rabacie")
    void exercise3() throws Exception {
        mockMvc.perform(get("/api/discount").param("price", "10000").param("percent", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discounted").value(8500));
    }

    @Test
    @DisplayName("Ćwiczenie 3: zły procent → 400 Bad Request")
    void exercise3BadRequest() throws Exception {
        mockMvc.perform(get("/api/discount").param("price", "10000").param("percent", "150"))
                .andExpect(status().isBadRequest());
    }
}
