package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Odczyt zamówień z pozycjami. Dwa sposoby, ten sam wynik: jedno zapytanie JOIN + ResultSetExtractor
 * albo zapytanie o zamówienia i osobne zapytanie o pozycje KAŻDEGO zamówienia (wzorzec „N+1”).
 */
public class Jdbc02OrderReader {

    private static final String ORDERS_WITH_LINES = """
            SELECT o.id AS order_id, c.name AS customer_name, o.order_date,
                   l.product_id, l.quantity, l.unit_price
            FROM orders o
            JOIN customer c   ON c.id = o.customer_id
            JOIN order_line l ON l.order_id = o.id
            ORDER BY o.id, l.id""";

    private final JdbcTemplate jdbcTemplate;

    public Jdbc02OrderReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * ResultSetExtractor dostaje CAŁY ResultSet naraz (RowMapper — jeden wiersz na raz) i sam zwraca wynik.
     * JOIN zwraca po wierszu na pozycję, a my chcemy jedno zamówienie z listą pozycji — dlatego grupujemy
     * wiersze po numerze zamówienia. LinkedHashMap zachowuje kolejność z ORDER BY.
     */
    public List<ShopOrder> ordersWithLines() {
        ResultSetExtractor<List<ShopOrder>> extractor = resultSet -> {
            Map<Long, ShopOrder> byId = new LinkedHashMap<>();
            while (resultSet.next()) { // przechodzimy wiersz po wierszu — to my sterujemy pętlą
                long orderId = resultSet.getLong("order_id");
                ShopOrder order = byId.get(orderId);
                if (order == null) { // pierwszy wiersz tego zamówienia → tworzymy je z pustą listą pozycji
                    order = new ShopOrder(orderId, resultSet.getString("customer_name"),
                            resultSet.getDate("order_date").toLocalDate(), new ArrayList<>());
                    byId.put(orderId, order);
                }
                order.lines().add(readLine(resultSet));
            }
            return new ArrayList<>(byId.values());
        };
        return jdbcTemplate.query(ORDERS_WITH_LINES, extractor);
    }

    /** Wzorzec „N+1”: 1 zapytanie o zamówienia + po jednym o pozycje każdego z N zamówień. */
    public List<ShopOrder> ordersWithLinesNaive() {
        List<ShopOrder> headers = jdbcTemplate.query("""
                SELECT o.id, c.name AS customer_name, o.order_date
                FROM orders o JOIN customer c ON c.id = o.customer_id
                ORDER BY o.id""",
                (rs, rowNum) -> new ShopOrder(rs.getLong("id"), rs.getString("customer_name"),
                        rs.getDate("order_date").toLocalDate(), new ArrayList<>()));
        for (ShopOrder header : headers) { // N dodatkowych zapytań, po jednym na zamówienie
            header.lines().addAll(jdbcTemplate.query(
                    "SELECT product_id, quantity, unit_price FROM order_line WHERE order_id = ? ORDER BY id",
                    (rs, rowNum) -> readLine(rs), header.id()));
        }
        return headers;
    }

    private static OrderLine readLine(ResultSet resultSet) throws SQLException {
        return new OrderLine(resultSet.getLong("product_id"), resultSet.getInt("quantity"),
                resultSet.getBigDecimal("unit_price"));
    }
}
