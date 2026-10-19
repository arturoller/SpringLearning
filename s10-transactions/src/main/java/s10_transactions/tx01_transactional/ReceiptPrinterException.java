package s10_transactions.tx01_transactional;

/**
 * Drukarka paragonów (receipt printer) nie działa. Wyjątek niekontrolowany, ale „niegroźny” dla pieniędzy: obciążenie
 * konta ma zostać, nawet gdy paragon się nie wydrukuje → {@code noRollbackFor = ReceiptPrinterException.class}.
 */
public class ReceiptPrinterException extends RuntimeException {

    public ReceiptPrinterException(String message) {
        super(message);
    }
}
