package s07_data_jdbc.jdbc01_jdbc_client;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * To samo zapytanie co {@link ProductQueries#findByCategory(String)}, ale czystym JDBC — dla porównania ilości kodu.
 * Szczegóły czystego JDBC: JavaLearning {@code t29_jdbc_databases/Jdbc03PreparedStatement}.
 */
public class PlainJdbcProducts {

    private final DataSource dataSource;

    public PlainJdbcProducts(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Product> findByCategory(String category) {
        List<Product> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM product WHERE category = ? ORDER BY name")) {
            statement.setString(1, category);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new Product(rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
                            rs.getString("category"), rs.getBigDecimal("price"), rs.getInt("stock")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Błąd zapytania", e);   // wyjątek kontrolowany trzeba obsłużyć ręcznie
        }
        return result;
    }
}
