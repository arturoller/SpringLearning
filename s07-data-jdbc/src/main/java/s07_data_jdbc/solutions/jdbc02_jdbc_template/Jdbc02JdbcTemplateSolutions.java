package s07_data_jdbc.solutions.jdbc02_jdbc_template;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import s07_data_jdbc.jdbc02_jdbc_template.Customer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc02JdbcTemplate — zajrzyj dopiero po własnej próbie! */
public final class Jdbc02JdbcTemplateSolutions {

    private Jdbc02JdbcTemplateSolutions() {
    }

    /** Rozwiązanie 1: COUNT(*) jako Integer (queryForObject zwraca typ opakowany). */
    public static int solution1(JdbcTemplate jdbcTemplate, String category) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE category = ?",
                Integer.class, category);
        return count == null ? 0 : count;
    }

    /** Rozwiązanie 2: query zwraca listę (może być pusta) — pierwszy element albo Optional.empty(). */
    public static Optional<Customer> solution2(JdbcTemplate jdbcTemplate, long id) {
        List<Customer> found = jdbcTemplate.query("SELECT * FROM customer WHERE id = ?",
                new DataClassRowMapper<>(Customer.class), id);
        return found.stream().findFirst();
    }

    /** Rozwiązanie 3: jedna paczka z nazwanymi parametrami, suma wyników. */
    public static int solution3(NamedParameterJdbcTemplate namedTemplate, Map<String, Integer> newStocks) {
        SqlParameterSource[] batch = newStocks.entrySet().stream()
                .map(entry -> new MapSqlParameterSource("sku", entry.getKey()).addValue("stock", entry.getValue()))
                .toArray(SqlParameterSource[]::new);
        int[] results = namedTemplate.batchUpdate("UPDATE product SET stock = :stock WHERE sku = :sku", batch);
        int sum = 0;
        for (int rows : results) {
            sum += rows;
        }
        return sum;
    }

    /** Rozwiązanie 4: szczegółowy wyjątek (DuplicateKeyException) PRZED ogólnym (jego nadklasą). */
    public static String solution4(JdbcTemplate jdbcTemplate, String number, long customerId) {
        try {
            jdbcTemplate.update("INSERT INTO shop_order (number, customer_id, status) VALUES (?, ?, 'NOWE')",
                    number, customerId);
            return "OK";
        } catch (DuplicateKeyException e) {
            return "DUPLIKAT";
        } catch (DataIntegrityViolationException e) {
            return "BRAK_KLIENTA";
        }
    }
}
