package s07_data_jdbc.solutions.jdbc02_jdbc_template;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import s07_data_jdbc.jdbc02_jdbc_template.Customer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc02JdbcTemplate — zajrzyj dopiero po własnej próbie! */
public final class Jdbc02JdbcTemplateSolutions {

    private static final RowMapper<Customer> MAPPER =
            (rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("city"));

    private Jdbc02JdbcTemplateSolutions() {
    }

    static List<Customer> solution1(JdbcTemplate jdbcTemplate, String city) {
        return jdbcTemplate.query("SELECT id, name, city FROM customers WHERE city = ? ORDER BY name", MAPPER, city);
    }

    static Optional<Customer> solution2(JdbcTemplate jdbcTemplate, long id) {
        try {
            return Optional.ofNullable(
                    jdbcTemplate.queryForObject("SELECT id, name, city FROM customers WHERE id = ?", MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();   // brak klienta to normalna sytuacja, nie błąd
        }
    }

    static int solution3(NamedParameterJdbcTemplate namedTemplate, List<Customer> customers) {
        int[] results = namedTemplate.batchUpdate("INSERT INTO customers (name, city) VALUES (:name, :city)",
                SqlParameterSourceUtils.createBatch(customers));
        return Arrays.stream(results).sum();
    }
}
