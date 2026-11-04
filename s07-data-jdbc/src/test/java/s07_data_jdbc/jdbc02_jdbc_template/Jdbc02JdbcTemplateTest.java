package s07_data_jdbc.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc02JdbcTemplate. Wycinek @JdbcTest (JdbcTemplate i NamedParameterJdbcTemplate są w nim
 * gotowe do wstrzyknięcia); każdy test w transakcji wycofywanej na końcu.
 */
@JdbcTest
@ActiveProfiles("jdbc02")
class Jdbc02JdbcTemplateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedTemplate;

    private Jdbc02ShopDao dao() {
        return new Jdbc02ShopDao(jdbcTemplate, namedTemplate);
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: query, queryForObject, queryForList, update — i NamedParameterJdbcTemplate opakowuje ten sam JdbcTemplate")
    void section1JdbcTemplate() {
        assertThat(dao().findByCategory("dom")).extracting(Product::name).containsExactly("Kubek", "Lampka");
        assertThat(dao().getById(2).name()).isEqualTo("Monitor");
        assertThat(dao().findById(2)).isPresent();
        assertThat(dao().findById(999)).isEmpty();
        assertThat(dao().countByCategory("komputery")).isEqualTo(2);
        assertThat(dao().names()).containsExactly("Mysz", "Monitor", "Kubek", "Lampka");
        assertThat(dao().changeStock(1, 25)).isEqualTo(1);
        assertThat(dao().getById(1).stock()).isEqualTo(25);
        assertThat(dao().changeStock(999, 1)).isZero();

        assertThat(namedTemplate.getJdbcOperations()).as("ten sam bean JdbcTemplate").isSameAs(jdbcTemplate);
    }

    @Test
    @DisplayName("Sekcja 1: queryForObject rzuca wyjątek dla 0 wierszy i dla 2+ wierszy")
    void section1QueryForObjectPitfall() {
        assertThatThrownBy(() -> dao().getById(999))
                .isInstanceOf(EmptyResultDataAccessException.class)
                .hasMessageContaining("expected 1, actual 0");
        assertThatThrownBy(() -> jdbcTemplate.queryForObject("SELECT name FROM product", String.class))
                .isExactlyInstanceOf(IncorrectResultSizeDataAccessException.class)
                .hasMessageContaining("expected 1, actual 4");
    }

    @Test
    @DisplayName("Sekcja 1: BeanPropertyRowMapper nie radzi sobie z rekordem, DataClassRowMapper tak")
    void section1RowMappers() {
        assertThatThrownBy(() -> jdbcTemplate.query("SELECT * FROM product", new BeanPropertyRowMapper<>(Product.class)))
                .isInstanceOf(BeanInstantiationException.class);
        List<Product> products = jdbcTemplate.query("SELECT * FROM product ORDER BY id", new DataClassRowMapper<>(Product.class));
        assertThat(products).hasSize(4);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: parametry nazwane, IN z listą i INSERT z obiektu")
    void section2NamedParameters() {
        assertThat(dao().search("komputery", new BigDecimal("50"), new BigDecimal("100")))
                .extracting(Product::name).containsExactly("Mysz");
        assertThat(dao().findByIds(List.of(3L, 1L))).extracting(Product::name).containsExactly("Mysz", "Kubek");

        assertThat(dao().insert(new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40))).isEqualTo(1);
        assertThat(dao().countByCategory("dom")).isEqualTo(3);
    }

    @Test
    @DisplayName("Sekcja 2: pusta lista w IN (:ids) jest rozwijana do IN () — H2 to toleruje, inne bazy nie")
    void section2EmptyInList() {
        String expanded = NamedParameterUtils.substituteNamedParameters(
                NamedParameterUtils.parseSqlStatement("SELECT * FROM product WHERE id IN (:ids)"),
                new MapSqlParameterSource("ids", List.of()));
        assertThat(expanded).endsWith("IN ()");
        assertThat(dao().findByIds(List.of())).as("H2 2.4 zwraca pusty wynik; PostgreSQL zgłosiłby błąd składni").isEmpty();
        assertThat(namedTemplate.queryForList("SELECT name FROM product WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", List.of(1L)), String.class)).containsExactly("Mysz");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: batchUpdate (pozycyjny i nazwany) wstawia wiele wierszy i zwraca tablicę liczników")
    void section3Batch() {
        List<Product> newProducts = List.of(
                new Product(null, "Zeszyt", "dom", new BigDecimal("4.50"), 40),
                new Product(null, "Długopis", "dom", new BigDecimal("2.00"), 200));

        assertThat(dao().insertAll(newProducts)).containsExactly(1, 1);
        assertThat(dao().insertAllNamed(newProducts)).containsExactly(1, 1);
        assertThat(dao().countByCategory("dom")).as("2 z danych + 2 + 2 z batchy").isEqualTo(6);
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: ResultSetExtractor składa zamówienia z pozycji z jednego JOIN-a")
    void section4ResultSetExtractor() {
        List<ShopOrder> orders = new Jdbc02OrderReader(jdbcTemplate).ordersWithLines();

        assertThat(orders).extracting(ShopOrder::id).containsExactly(1L, 2L, 3L);
        assertThat(orders).extracting(ShopOrder::customerName).containsExactly("Anna Nowak", "Jan Kowalski", "Anna Nowak");
        assertThat(orders.get(0).orderDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(orders).extracting(o -> o.lines().size()).containsExactly(2, 1, 3);
        assertThat(orders.get(1).lines().get(0).unitPrice()).isEqualByComparingTo("899.99");
    }

    @Test
    @DisplayName("Sekcja 4: wzorzec N+1 daje te same dane, tylko kosztem 1 + N zapytań")
    void section4NPlusOneGivesSameData() {
        Jdbc02OrderReader reader = new Jdbc02OrderReader(jdbcTemplate);
        assertThat(reader.ordersWithLinesNaive()).isEqualTo(reader.ordersWithLines());
    }
}
