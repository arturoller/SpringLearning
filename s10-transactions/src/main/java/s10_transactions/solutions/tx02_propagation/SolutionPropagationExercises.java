package s10_transactions.solutions.tx02_propagation;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s10_transactions.tx02_propagation.StockService;

import java.util.List;

/** Rozwiązania ćwiczeń 2 i 4 (część 2) lekcji Tx02Propagation. */
@Service
public class SolutionPropagationExercises {

    private final JdbcClient jdbc;
    private final StockService stock;
    private final SolutionOrderWriter writer;

    public SolutionPropagationExercises(JdbcClient jdbc, StockService stock, SolutionOrderWriter writer) {
        this.jdbc = jdbc;
        this.stock = stock;
        this.writer = writer;
    }

    /** ĆWICZENIE 2: tryReserve zwraca wynik — żaden wyjątek nie przechodzi przez proxy, transakcja nie jest „zatruta”. */
    @Transactional
    public String placeOrderOrWait(String orderNumber, String product, int quantity) {
        jdbc.sql("INSERT INTO shop_order (order_number, status) VALUES (?, 'NEW')").param(orderNumber).update();
        String status = stock.tryReserve(product, quantity) ? "RESERVED" : "WAITING";
        jdbc.sql("UPDATE shop_order SET status = ? WHERE order_number = ?").params(status, orderNumber).update();
        return status;
    }

    /**
     * ĆWICZENIE 4: każde zamówienie zatwierdza writer (REQUIRES_NEW), więc wyjątek na końcu importu wycofuje tylko
     * transakcję tej metody — a ona nic nie zapisała. Duplikat wycofuje tylko SWOJĄ nową transakcję; wyjątek
     * przeszedł przez proxy REQUIRES_NEW, które nie oznacza zewnętrznej transakcji jako rollback-only.
     */
    @Transactional
    public int importOrders(List<String> orderNumbers) {
        int imported = 0;
        for (String number : orderNumbers) {
            if (number == null || number.isBlank()) {
                throw new IllegalArgumentException("Pusty numer zamówienia po " + imported + " zaimportowanych");
            }
            try {
                writer.insertImported(number);
                imported++;
            } catch (DuplicateKeyException e) {
                // duplikat — pomijamy
            }
        }
        return imported;
    }
}
