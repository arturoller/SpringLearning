package s07_data_jdbc.jdbc03_sql_init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc03SqlInit. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("jdbc03")
class Jdbc03SqlInitExercisesTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("Ćwiczenie 1: nazwy kategorii z data.sql")
    void exercise1() {
        assertThat(Jdbc03SqlInit.exercise1(jdbcClient)).containsExactly("Nabial", "Napoje", "Slodycze");
    }

    @Test
    @DisplayName("Ćwiczenie 2: czy tabela powstała w danym trybie")
    void exercise2() {
        assertThat(Jdbc03SqlInit.exercise2("embedded")).isTrue();
        assertThat(Jdbc03SqlInit.exercise2("always")).isTrue();
        assertThat(Jdbc03SqlInit.exercise2("never")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: skrypt z błędem mimo wszystko tworzy poprawne tabele")
    void exercise3() {
        assertThat(Jdbc03SqlInit.exercise3()).containsExactly("LEGACY_A", "LEGACY_B");
    }
}
