package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Wspólne polecenia SQL lekcji Tx05 (żeby klasy z przykładami pokazywały transakcje, a nie SQL). */
final class OrderSql {

    private OrderSql() {
    }

    static void insertOrder(JdbcClient jdbc, OrderLine line, String status) {
        jdbc.sql("INSERT INTO customer_order (order_number, amount, status) VALUES (?, ?, ?)")
                .param(line.number()).param(line.amount()).param(status).update();
    }

    static void insertAudit(JdbcClient jdbc, String message) {
        jdbc.sql("INSERT INTO audit_log (message) VALUES (?)").param(message).update();
    }

    static long countOrders(JdbcClient jdbc) {
        return jdbc.sql("SELECT COUNT(*) FROM customer_order").query(Long.class).single();
    }
}
