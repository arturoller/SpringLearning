package s04_web_rest.solutions.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Rest04JsonExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Rest04JsonSolutionsTest {

    final JsonMapper mapper = JsonMapper.builder().build();

    record Delivery(String city, String note, LocalDate day) {
    }

    @Test
    @DisplayName("Wzorzec 1: full_name, email pomijany gdy null, hasło nigdy")
    void solution1() {
        assertThat(Rest04JsonSolutions.solution1(mapper, new SolutionCustomerJson("Anna Nowak", null, "tajne")))
                .isEqualTo("{\"full_name\":\"Anna Nowak\"}");
        assertThat(Rest04JsonSolutions.solution1(mapper, new SolutionCustomerJson("Jan Kos", "jan@example.com", "tajne")))
                .isEqualTo("{\"full_name\":\"Jan Kos\",\"email\":\"jan@example.com\"}");
    }

    @Test
    @DisplayName("Wzorzec 2: suma cen z tablicy JSON")
    void solution2() {
        assertThat(Rest04JsonSolutions.solution2(mapper,
                "[{\"name\":\"Kawa\",\"priceGrosze\":2999},{\"name\":\"Herbata\",\"priceGrosze\":1599}]")).isEqualTo(4598);
        assertThat(Rest04JsonSolutions.solution2(mapper, "[]")).isZero();
    }

    @Test
    @DisplayName("Wzorzec 3: surowy JsonMapper — nieznane pola → wyjątek, null pomijany, daty ISO")
    void solution3() {
        JsonMapper strict = Rest04JsonSolutions.solution3();

        assertThat(strict.writeValueAsString(new Delivery("Kraków", null, LocalDate.of(2026, 10, 20))))
                .isEqualTo("{\"city\":\"Kraków\",\"day\":\"2026-10-20\"}");
        assertThatThrownBy(() -> strict.readValue("{\"city\":\"Kraków\",\"floor\":3}", Delivery.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }
}
