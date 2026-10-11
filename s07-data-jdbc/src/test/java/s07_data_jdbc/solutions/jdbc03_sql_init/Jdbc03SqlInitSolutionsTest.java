package s07_data_jdbc.solutions.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s07_data_jdbc.jdbc03_sql_init.Jdbc03Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jdbc03SqlInitExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Jdbc03Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Jdbc03Application.class)
@ActiveProfiles("jdbc03")
class Jdbc03SqlInitSolutionsTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Wzorzec 1: nazwy kategorii z data.sql")
    void solution1() {
        assertThat(Jdbc03SqlInitSolutions.solution1(jdbcClient)).containsExactly("Nabial", "Napoje", "Slodycze");
    }

    @Test
    @DisplayName("Wzorzec 2: czy tabela powstała w danym trybie")
    void solution2() {
        assertThat(Jdbc03SqlInitSolutions.solution2("embedded")).isTrue();
        assertThat(Jdbc03SqlInitSolutions.solution2("always")).isTrue();
        assertThat(Jdbc03SqlInitSolutions.solution2("never")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 3: skrypt z błędem mimo wszystko tworzy poprawne tabele")
    void solution3() {
        assertThat(Jdbc03SqlInitSolutions.solution3()).containsExactly("LEGACY_A", "LEGACY_B");
    }
}
