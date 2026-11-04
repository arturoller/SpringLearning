package s07_data_jdbc.solutions.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import s07_data_jdbc.jdbc02_jdbc_template.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Jdbc02JdbcTemplateExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@JdbcTest
@ActiveProfiles("jdbc02")
class Jdbc02JdbcTemplateSolutionsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate;

    @Test
    @DisplayName("Wzorzec 1: nazwy produktów z kategorii, alfabetycznie")
    void solution1() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution1NamesInCategory(jdbcTemplate, "dom")).containsExactly("Kubek", "Lampka");
        assertThat(Jdbc02JdbcTemplateSolutions.solution1NamesInCategory(jdbcTemplate, "ogród")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: produkt po id bez wyjątku dla braku")
    void solution2() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution2FindProduct(jdbcTemplate, 2)).get()
                .extracting(Product::name).isEqualTo("Monitor");
        assertThat(Jdbc02JdbcTemplateSolutions.solution2FindProduct(jdbcTemplate, 999)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 3: produkty po liście numerów (także pustej)")
    void solution3() {
        assertThat(Jdbc02JdbcTemplateSolutions.solution3ProductsByIds(namedTemplate, List.of(4L, 1L)))
                .extracting(Product::name).containsExactly("Mysz", "Lampka");
        assertThat(Jdbc02JdbcTemplateSolutions.solution3ProductsByIds(namedTemplate, List.of())).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 4: wsadowe wstawianie produktów")
    void solution4() {
        List<Product> products = List.of(
                new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40),
                new Product(null, "Długopis", "dom", new BigDecimal("2.00"), 200),
                new Product(null, "Teczka", "dom", new BigDecimal("7.00"), 10));
        assertThat(Jdbc02JdbcTemplateSolutions.solution4InsertAll(jdbcTemplate, products)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Integer.class)).isEqualTo(7);
    }

    @Test
    @DisplayName("Wzorzec 5: wartość zamówień policzona w Javie z jednego zapytania")
    void solution5() {
        Map<Long, BigDecimal> totals = Jdbc02JdbcTemplateSolutions.solution5OrderTotals(jdbcTemplate);
        assertThat(totals.keySet()).containsExactly(1L, 2L, 3L);
        assertThat(totals.get(1L)).isEqualByComparingTo("142.50");
        assertThat(totals.get(2L)).isEqualByComparingTo("899.99");
        assertThat(totals.get(3L)).isEqualByComparingTo("315.00");
    }
}
