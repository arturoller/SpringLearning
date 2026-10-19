package s17_events.evt02_transactional_events;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/** Repozytorium lekcji: jedyne miejsce z SQL (tabele orders i loyalty_points z migracji V1). */
@Repository
public class OrderRepository {

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertOrder(String orderId, String customer, BigDecimal totalPln) {
        jdbc.sql("INSERT INTO orders (id, customer, total) VALUES (:id, :customer, :total)")
                .param("id", orderId).param("customer", customer).param("total", totalPln).update();
    }

    public void insertPoints(String orderId, String customer, int points) {
        jdbc.sql("INSERT INTO loyalty_points (order_id, customer, points) VALUES (:orderId, :customer, :points)")
                .param("orderId", orderId).param("customer", customer).param("points", points).update();
    }

    public int countOrders() {
        return jdbc.sql("SELECT COUNT(*) FROM orders").query(Integer.class).single();
    }

    public int countPoints() {
        return jdbc.sql("SELECT COUNT(*) FROM loyalty_points").query(Integer.class).single();
    }

    public void deleteAll() {
        jdbc.sql("DELETE FROM loyalty_points").update();
        jdbc.sql("DELETE FROM orders").update();
    }
}
