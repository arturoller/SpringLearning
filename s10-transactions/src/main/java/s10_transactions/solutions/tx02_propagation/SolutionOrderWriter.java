package s10_transactions.solutions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Rozwiązanie ćwiczenia 4 (część 1) lekcji Tx02Propagation: każde zamówienie w osobnej, nowej transakcji. */
@Service
public class SolutionOrderWriter {

    private final JdbcClient jdbc;

    public SolutionOrderWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** REQUIRES_NEW — osobny bean, więc wywołanie idzie przez proxy (sekcja 5: przez this by nie zadziałało). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertImported(String orderNumber) {
        jdbc.sql("INSERT INTO shop_order (order_number, status) VALUES (?, 'IMPORTED')").param(orderNumber).update();
    }
}
