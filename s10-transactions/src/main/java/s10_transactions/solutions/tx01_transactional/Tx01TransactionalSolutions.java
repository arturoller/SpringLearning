package s10_transactions.solutions.tx01_transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s10_transactions.tx01_transactional.Account;
import s10_transactions.tx01_transactional.AccountNotFoundException;
import s10_transactions.tx01_transactional.AccountRepository;
import s10_transactions.tx01_transactional.PaymentGatewayException;
import s10_transactions.tx01_transactional.ReceiptPrinterException;

import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Tx01Transactional — zajrzyj dopiero po własnej próbie!
 * To bean ({@code @Service}), ale w pakiecie solutions, którego Tx01Application NIE skanuje — test rozwiązań
 * rejestruje go sam: {@code @SpringBootTest(classes = {Tx01Application.class, Tx01TransactionalSolutions.class})}.
 */
@Service
public class Tx01TransactionalSolutions {

    private final AccountRepository accounts;

    public Tx01TransactionalSolutions(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(rollbackFor = PaymentGatewayException.class)
    public void transferWithLimit(String from, String to, long amount) throws PaymentGatewayException {
        find(from).debit(amount);
        find(to).credit(amount);
        if (amount > 50000) {
            throw new PaymentGatewayException("Przekroczony limit");
        }
    }

    @Transactional(noRollbackFor = ReceiptPrinterException.class)
    public void chargeWithBrokenPrinter(String number, long amount) {
        find(number).debit(amount);
        throw new ReceiptPrinterException("Drukarka offline");
    }

    @Transactional
    public long payAll(String from, Map<String, Long> payments) {
        Account source = find(from);
        long total = 0;
        for (Map.Entry<String, Long> payment : payments.entrySet()) {
            source.debit(payment.getValue());
            find(payment.getKey()).credit(payment.getValue());
            total += payment.getValue();
        }
        return total;
    }

    private Account find(String number) {
        return accounts.findById(number).orElseThrow(() -> new AccountNotFoundException(number));
    }
}
