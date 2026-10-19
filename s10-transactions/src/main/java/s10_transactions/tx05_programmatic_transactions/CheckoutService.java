package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/**
 * CheckoutService = finalizacja zamówienia: płatność w zewnętrznej bramce + zapis zamówienia w bazie. Pokazuje,
 * po co transakcja programowa: żeby transakcja (i połączenie z bazą) obejmowała TYLKO pracę z bazą, a nie wolne
 * wywołanie innego systemu.
 */
@Service
public class CheckoutService {

    private final PaymentGateway paymentGateway;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;

    public CheckoutService(PaymentGateway paymentGateway, JdbcClient jdbc, TransactionTemplate transactionTemplate) {
        this.paymentGateway = paymentGateway;
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
    }

    private void saveOrder(String customer, BigDecimal total, String paymentId) {
        jdbc.sql("insert into orders (customer, total, payment_id) values (?, ?, ?)")
                .params(customer, total, paymentId)
                .update();
    }

    /**
     * PRZED: @Transactional na całej metodzie — transakcja (i połączenie z puli) trwa także podczas wywołania bramki.
     * Gdy bramka odpowiada 5 s, połączenie jest zajęte 5 s, choć baza w tym czasie nic nie robi.
     */
    @Transactional
    public String checkoutInsideTransaction(String customer, BigDecimal total) {
        String paymentId = paymentGateway.charge(customer, total);
        saveOrder(customer, total, paymentId);
        return paymentId;
    }

    /**
     * PO: płatność POZA transakcją, a krótka transakcja tylko na zapis. Połączenie jest zajęte tylko na czas INSERT.
     * (Uwaga projektowa: gdyby zapis się nie udał, płatność już przeszła — trzeba by ją wycofać osobnym wywołaniem
     * bramki; to tzw. kompensacja.)
     */
    public String checkoutWithShortTransaction(String customer, BigDecimal total) {
        String paymentId = paymentGateway.charge(customer, total);
        transactionTemplate.executeWithoutResult(status -> saveOrder(customer, total, paymentId));
        return paymentId;
    }
}
