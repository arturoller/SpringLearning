package s07_data_jdbc.solutions.jdbc02_jdbc_template;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import s07_data_jdbc.jdbc02_jdbc_template.Product;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc02JdbcTemplate — zajrzyj dopiero po własnej próbie! */
public final class Jdbc02JdbcTemplateSolutions {

    private Jdbc02JdbcTemplateSolutions() {
    }

    /** Rozwiązanie 1: queryForList z klasą wartości; kategoria jako parametr, sortowanie w SQL. */
    public static List<String> solution1NamesInCategory(JdbcTemplate jdbcTemplate, String category) {
        return jdbcTemplate.queryForList("SELECT name FROM product WHERE category = ? ORDER BY name", String.class, category);
    }

    /** Rozwiązanie 2: query() zwraca listę (pustą dla braku) — zamiast queryForObject, które rzuca wyjątek. */
    public static Optional<Product> solution2FindProduct(JdbcTemplate jdbcTemplate, long id) {
        return jdbcTemplate.query("SELECT * FROM product WHERE id = ?", new DataClassRowMapper<>(Product.class), id)
                .stream().findFirst();
    }

    /** Rozwiązanie 3: IN (:ids) rozwija lista; pustą listę obsługujemy przed zapytaniem. */
    public static List<Product> solution3ProductsByIds(NamedParameterJdbcTemplate namedTemplate, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return namedTemplate.query("SELECT * FROM product WHERE id IN (:ids) ORDER BY id",
                new MapSqlParameterSource("ids", ids), new DataClassRowMapper<>(Product.class));
    }

    /** Rozwiązanie 4: jedno przygotowane polecenie, wiele zestawów parametrów; suma liczników. */
    public static int solution4InsertAll(JdbcTemplate jdbcTemplate, List<Product> products) {
        List<Object[]> argumentSets = products.stream()
                .map(p -> new Object[]{p.name(), p.category(), p.price(), p.stock()})
                .toList();
        int[] counts = jdbcTemplate.batchUpdate(
                "INSERT INTO product (name, category, price, stock) VALUES (?, ?, ?, ?)", argumentSets);
        return Arrays.stream(counts).sum();
    }

    /** Rozwiązanie 5: ResultSetExtractor sam prowadzi pętlę po wierszach i sumuje ilość × cena per zamówienie. */
    public static Map<Long, BigDecimal> solution5OrderTotals(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query(
                "SELECT order_id, quantity, unit_price FROM order_line ORDER BY order_id, id",
                resultSet -> {
                    Map<Long, BigDecimal> totals = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        BigDecimal lineValue = resultSet.getBigDecimal("unit_price")
                                .multiply(BigDecimal.valueOf(resultSet.getInt("quantity")));
                        totals.merge(resultSet.getLong("order_id"), lineValue, BigDecimal::add);
                    }
                    return totals;
                });
    }
}
