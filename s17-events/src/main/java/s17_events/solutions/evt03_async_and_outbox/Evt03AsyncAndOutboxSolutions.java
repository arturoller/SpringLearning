package s17_events.solutions.evt03_async_and_outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import s17_events.evt03_async_and_outbox.OutboxMessage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Rozwiązania wzorcowe ćwiczeń lekcji Evt03AsyncAndOutbox — zajrzyj dopiero po własnej próbie! */
public final class Evt03AsyncAndOutboxSolutions {

    private Evt03AsyncAndOutboxSolutions() {
    }

    /** Rozwiązanie 1: oba INSERT-y w jednej transakcji; wyjątek z lambdy wycofuje całość. */
    public static void solution1(TransactionTemplate transactions, JdbcClient jdbc, String orderId, String customer,
                                 boolean failAfterInserts) {
        transactions.executeWithoutResult(status -> {
            jdbc.sql("INSERT INTO orders (id, customer, total) VALUES (:id, :customer, :total)")
                    .param("id", orderId).param("customer", customer).param("total", new BigDecimal("349.90")).update();
            jdbc.sql("INSERT INTO outbox (order_id, type, payload) VALUES (:orderId, 'OrderPlaced', :payload)")
                    .param("orderId", orderId).param("payload", "orderId=" + orderId + ";customer=" + customer).update();
            if (failAfterInserts) {
                throw new IllegalStateException("Płatność odrzucona");
            }
        });
    }

    /** Rozwiązanie 2: odhaczenie dopiero po udanej wysyłce, błąd = licznik prób + następna wiadomość. */
    public static int solution2(JdbcClient jdbc, Consumer<OutboxMessage> sender) {
        List<OutboxMessage> unsent = jdbc.sql(
                        "SELECT id, order_id, type, payload, attempts FROM outbox WHERE sent = FALSE ORDER BY id")
                .query(OutboxMessage.class).list();
        int sent = 0;
        for (OutboxMessage message : unsent) {
            try {
                sender.accept(message);
                jdbc.sql("UPDATE outbox SET sent = TRUE WHERE id = :id").param("id", message.id()).update();
                sent++;
            } catch (RuntimeException e) {
                jdbc.sql("UPDATE outbox SET attempts = attempts + 1 WHERE id = :id").param("id", message.id()).update();
            }
        }
        return sent;
    }

    /** Rozwiązanie 3: {@code Set.add} zwraca false, gdy id było już w zbiorze — duplikat ignorujemy. */
    public static boolean solution3(Set<Long> processedIds, OutboxMessage message, Consumer<OutboxMessage> handler) {
        if (!processedIds.add(message.id())) {
            return false;
        }
        handler.accept(message);
        return true;
    }

    /** Rozwiązanie 4: jak rozwiązanie 2, ale zapytanie pomija wiadomości z wyczerpanym limitem prób. */
    public static int solution4(JdbcClient jdbc, Consumer<OutboxMessage> sender, int maxAttempts) {
        List<OutboxMessage> unsent = jdbc.sql(
                        "SELECT id, order_id, type, payload, attempts FROM outbox "
                                + "WHERE sent = FALSE AND attempts < :maxAttempts ORDER BY id")
                .param("maxAttempts", maxAttempts)
                .query(OutboxMessage.class).list();
        int sent = 0;
        for (OutboxMessage message : unsent) {
            try {
                sender.accept(message);
                jdbc.sql("UPDATE outbox SET sent = TRUE WHERE id = :id").param("id", message.id()).update();
                sent++;
            } catch (RuntimeException e) {
                jdbc.sql("UPDATE outbox SET attempts = attempts + 1 WHERE id = :id").param("id", message.id()).update();
            }
        }
        return sent;
    }
}
