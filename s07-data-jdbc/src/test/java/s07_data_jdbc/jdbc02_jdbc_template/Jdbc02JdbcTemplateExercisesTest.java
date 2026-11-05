package s07_data_jdbc.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Jdbc02JdbcTemplate. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@JdbcTest
@ActiveProfiles("jdbc02")
class Jdbc02JdbcTemplateExercisesTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate;

    @Test
    @DisplayName("Ćwiczenie 1: nazwy produktów z kategorii, alfabetycznie")
    void exercise1() {
        assertThat(Jdbc02JdbcTemplate.exercise1NamesInCategory(jdbcTemplate, "dom")).containsExactly("Kubek", "Lampka");
        assertThat(Jdbc02JdbcTemplate.exercise1NamesInCategory(jdbcTemplate, "ogród")).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: produkt po id bez wyjątku dla braku")
    void exercise2() {
        assertThat(Jdbc02JdbcTemplate.exercise2FindProduct(jdbcTemplate, 2)).get()
                .extracting(Product::name).isEqualTo("Monitor");
        assertThat(Jdbc02JdbcTemplate.exercise2FindProduct(jdbcTemplate, 999)).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: produkty po liście numerów (także pustej)")
    void exercise3() {
        assertThat(Jdbc02JdbcTemplate.exercise3ProductsByIds(namedTemplate, List.of(4L, 1L)))
                .extracting(Product::name).containsExactly("Mysz", "Lampka");
        assertThat(Jdbc02JdbcTemplate.exercise3ProductsByIds(namedTemplate, List.of())).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 4: wsadowe wstawianie produktów")
    void exercise4() {
        List<Product> products = List.of(
                new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40),
                new Product(null, "Długopis", "dom", new BigDecimal("2.00"), 200),
                new Product(null, "Teczka", "dom", new BigDecimal("7.00"), 10));
        assertThat(Jdbc02JdbcTemplate.exercise4InsertAll(jdbcTemplate, products)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Integer.class)).isEqualTo(7);
    }

    @Test
    @DisplayName("Ćwiczenie 5: wartość zamówień policzona w Javie z jednego zapytania")
    void exercise5() {
        Map<Long, BigDecimal> totals = Jdbc02JdbcTemplate.exercise5OrderTotals(jdbcTemplate);
        assertThat(totals.keySet()).containsExactly(1L, 2L, 3L);
        assertThat(totals.get(1L)).isEqualByComparingTo("142.50");
        assertThat(totals.get(2L)).isEqualByComparingTo("899.99");
        assertThat(totals.get(3L)).isEqualByComparingTo("315.00");
    }
}
