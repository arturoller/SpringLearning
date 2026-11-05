package s10_transactions.tx01_transactional;

/**
 * Wypłata odrzucona (withdrawal rejected = wypłata odrzucona). Wyjątek niesprawdzany, ale w
 * {@code FeeService.withdrawOrKeepFee} oznaczony {@code noRollbackFor} — transakcja i tak się ZATWIERDZI
 * (opłata za próbę zostaje). Sekcja 3 lekcji.
 */
public class WithdrawalRejectedException extends RuntimeException {

    public WithdrawalRejectedException(String message) {
        super(message);
    }
}
