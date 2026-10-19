package s10_transactions.tx05_programmatic_transactions;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/**
 * PaymentGateway = bramka płatności, czyli ZEWNĘTRZNY system (w prawdziwej aplikacji: wywołanie HTTP do operatora
 * płatności, które może trwać sekundy). Tu tylko udajemy płatność, ale zapamiętujemy, czy w chwili wywołania trwała
 * transakcja bazy — a z nią zajęte połączenie z puli (Tx02Propagation, sekcja 2).
 */
@Component
public class PaymentGateway {

    private int payments;
    private boolean transactionActiveDuringLastCharge;

    /** Obciąż klienta; zwraca numer płatności nadany przez bramkę. */
    public String charge(String customer, BigDecimal amount) {
        transactionActiveDuringLastCharge = TransactionSynchronizationManager.isActualTransactionActive();
        payments++;
        return "PAY-" + payments;
    }

    public boolean wasTransactionActiveDuringLastCharge() {
        return transactionActiveDuringLastCharge;
    }
}
