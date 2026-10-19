package s17_events.evt03_async_and_outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/** Repozytorium lekcji: jedyne miejsce z SQL (tabele orders i outbox z migracji V1). */
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

    public void insertOutbox(String orderId, String type, String payload) {
        jdbc.sql("INSERT INTO outbox (order_id, type, payload) VALUES (:orderId, :type, :payload)")
                .param("orderId", orderId).param("type", type).param("payload", payload).update();
    }

    /** Niewysłane wiadomości w kolejności zapisu (rosnące id). */
    public List<OutboxMessage> findUnsent() {
        return jdbc.sql("SELECT id, order_id, type, payload, attempts FROM outbox WHERE sent = FALSE ORDER BY id")
                .query((rs, rowNum) -> new OutboxMessage(rs.getLong("id"), rs.getString("order_id"),
                        rs.getString("type"), rs.getString("payload"), rs.getInt("attempts")))
                .list();
    }

    public void markSent(long id) {
        jdbc.sql("UPDATE outbox SET sent = TRUE WHERE id = :id").param("id", id).update();
    }

    public void incrementAttempts(long id) {
        jdbc.sql("UPDATE outbox SET attempts = attempts + 1 WHERE id = :id").param("id", id).update();
    }

    public int countOrders() {
        return jdbc.sql("SELECT COUNT(*) FROM orders").query(Integer.class).single();
    }

    public int countOutbox() {
        return jdbc.sql("SELECT COUNT(*) FROM outbox").query(Integer.class).single();
    }

    public void deleteAll() {
        jdbc.sql("DELETE FROM outbox").update();
        jdbc.sql("DELETE FROM orders").update();
    }
}
