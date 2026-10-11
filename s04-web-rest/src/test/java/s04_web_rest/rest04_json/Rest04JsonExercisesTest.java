package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Rest04Json. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Bez Springa. */
@Tag("cwiczenie")
class Rest04JsonExercisesTest {

    final JsonMapper mapper = JsonMapper.builder().build();

    /** Mały rekord do ćwiczenia 3. */
    record Delivery(String city, String note, LocalDate day) {
    }

    @Test
    @DisplayName("Ćwiczenie 1: full_name, email pomijany gdy null, hasło nigdy")
    void exercise1() {
        assertThat(Rest04Json.exercise1(mapper, new CustomerJson("Anna Nowak", null, "tajne")))
                .isEqualTo("{\"full_name\":\"Anna Nowak\"}");
        assertThat(Rest04Json.exercise1(mapper, new CustomerJson("Jan Kos", "jan@example.com", "tajne")))
                .isEqualTo("{\"full_name\":\"Jan Kos\",\"email\":\"jan@example.com\"}");
    }

    @Test
    @DisplayName("Ćwiczenie 2: suma cen z tablicy JSON")
    void exercise2() {
        assertThat(Rest04Json.exercise2(mapper,
                "[{\"name\":\"Kawa\",\"priceGrosze\":2999},{\"name\":\"Herbata\",\"priceGrosze\":1599}]")).isEqualTo(4598);
        assertThat(Rest04Json.exercise2(mapper, "[]")).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 3: surowy JsonMapper — nieznane pola → wyjątek, null pomijany, daty ISO")
    void exercise3() {
        JsonMapper strict = Rest04Json.exercise3();

        assertThat(strict.writeValueAsString(new Delivery("Kraków", null, LocalDate.of(2026, 10, 20))))
                .isEqualTo("{\"city\":\"Kraków\",\"day\":\"2026-10-20\"}");
        assertThatThrownBy(() -> strict.readValue("{\"city\":\"Kraków\",\"floor\":3}", Delivery.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }
}
