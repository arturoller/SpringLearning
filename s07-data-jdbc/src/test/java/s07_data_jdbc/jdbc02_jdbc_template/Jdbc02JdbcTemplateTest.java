package s07_data_jdbc.jdbc02_jdbc_template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc02JdbcTemplate. {@code @SpringBootTest} + profil jdbc02; {@code @Transactional} cofa zmiany po
 * każdym teście (kolejność testów nie ma znaczenia).
 */
@SpringBootTest
@ActiveProfiles("jdbc02")
@Transactional
class Jdbc02JdbcTemplateTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedTemplate;

    @Autowired
    JdbcClient jdbcClient;

    // mapper wiersza jako lambda: RowMapper ma jedną metodę, więc pasuje lambda
    private final RowMapper<Customer> mapper =
            (rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city"));

    @Test
    @DisplayName("1. query + RowMapper (lambda) mapuje wiersze na rekordy; parametry pozycyjne idą na końcu")
    void queryWithRowMapper() {
        List<Customer> warsaw = jdbcTemplate.query(
                "SELECT id, name, city FROM customers WHERE city = ? ORDER BY name", mapper, "Warszawa");
        assertThat(warsaw).extracting(Customer::name).containsExactly("Anna Nowak", "Ewa Wisniewska");

        // odwrócona kolejność parametrów: tekst tam, gdzie baza chce liczby (pytanie kontrolne 4)
        assertThatThrownBy(() -> jdbcTemplate.query(
                "SELECT id, name, city FROM customers WHERE id > ? AND city = ?", mapper, "Warszawa", 1))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("2. queryForObject: 0 wierszy to EmptyResult..., 2+ wierszy to IncorrectResultSize...; JdbcClient ma optional()")
    void queryForObjectAndOptional() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
        assertThat(count).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT name FROM customers WHERE id = ?", String.class, 1))
                .isEqualTo("Anna Nowak");

        assertThatThrownBy(() -> jdbcTemplate.queryForObject(
                "SELECT name FROM customers WHERE id = ?", String.class, 99))
                .isInstanceOf(EmptyResultDataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.queryForObject("SELECT name FROM customers", String.class))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class)
                .isNotInstanceOf(EmptyResultDataAccessException.class);

        // JdbcClient: brak wiersza to Optional.empty(), a 2+ wiersze nadal wyjątek
        assertThat(jdbcClient.sql("SELECT name FROM customers WHERE id = :id").param("id", 99)
                .query(String.class).optional()).isEmpty();
        assertThatThrownBy(() -> jdbcClient.sql("SELECT name FROM customers").query(String.class).optional())
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
        assertThatThrownBy(() -> jdbcClient.sql("SELECT name FROM customers WHERE id = 99")
                .query(String.class).single())
                .isInstanceOf(EmptyResultDataAccessException.class);
    }

    @Test
    @DisplayName("3. NamedParameterJdbcTemplate: parametry nazwane z mapy, kolejność nie ma znaczenia")
    void namedParameters() {
        List<Customer> found = namedTemplate.query(
                "SELECT id, name, city FROM customers WHERE city = :city AND id > :minId ORDER BY id",
                Map.of("minId", 1, "city", "Warszawa"), mapper);
        assertThat(found).extracting(Customer::name).containsExactly("Ewa Wisniewska");
    }

    @Test
    @DisplayName("4. batchUpdate wstawia wiele zestawów parametrów naraz i zwraca po jednym wyniku na zestaw")
    void batchUpdate() {
        int[] positional = jdbcTemplate.batchUpdate("INSERT INTO customers (name, city) VALUES (?, ?)",
                List.of(new Object[]{"Adam", "Lodz"}, new Object[]{"Bartek", "Lodz"}, new Object[]{"Celina", "Lodz"}));
        assertThat(positional).containsExactly(1, 1, 1);

        int[] named = namedTemplate.batchUpdate("INSERT INTO customers (name, city) VALUES (:name, :city)",
                SqlParameterSourceUtils.createBatch(List.of(new Customer(0, "Dorota", "Lodz"), new Customer(0, "Edek", "Lodz"))));
        assertThat(named).containsExactly(1, 1);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers WHERE city = 'Lodz'", Integer.class))
                .isEqualTo(5);
    }

    @Test
    @DisplayName("5. Boot tworzy wszystkie trzy narzędzia na tym samym DataSource - można je mieszać")
    void allThreeToolsShareOneDataSource() {
        jdbcTemplate.update("INSERT INTO customers (name, city) VALUES (?, ?)", "Franek", "Sopot");
        assertThat(namedTemplate.queryForObject("SELECT COUNT(*) FROM customers WHERE city = :c",
                Map.of("c", "Sopot"), Integer.class)).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM customers WHERE city = ?").param("Sopot")
                .query(Integer.class).single()).isEqualTo(1);
    }
}
