package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ZWYKŁY obiekt (nie bean — tworzony przez {@code new}), który i tak ma transakcje: {@code TransactionTemplate} nie
 * potrzebuje proxy, więc działa też w obiekcie z {@code new}, w metodzie prywatnej i przy wywołaniu
 * {@code this.metoda()} — tam, gdzie {@code @Transactional} milczy (s10_transactions/Tx03ProxyPitfalls).
 */
public class PlainOrderWriter {

    private final TransactionTemplate tx;
    private final JdbcClient jdbc;

    public PlainOrderWriter(TransactionTemplate tx, JdbcClient jdbc) {
        this.tx = tx;
        this.jdbc = jdbc;
    }

    /** Dwa zamówienia w JEDNEJ transakcji — przez prywatną metodę wywołaną przez this. */
    public void writePair(OrderLine first, OrderLine second) {
        this.writeBoth(first, second);
    }

    private void writeBoth(OrderLine first, OrderLine second) {
        tx.executeWithoutResult(status -> {
            OrderSql.insertOrder(jdbc, first, "NEW");
            OrderSql.insertOrder(jdbc, second, "NEW"); // błąd tu → wycofane OBA
        });
    }
}
