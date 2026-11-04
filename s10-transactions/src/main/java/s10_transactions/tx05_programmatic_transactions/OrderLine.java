package s10_transactions.tx05_programmatic_transactions;

import java.math.BigDecimal;

/**
 * Jedna pozycja do zaimportowania: numer zamówienia i kwota. Kwota ≤ 0 albo powtórzony numer → błąd bazy (CHECK /
 * UNIQUE) w środku transakcji.
 */
public record OrderLine(String number, BigDecimal amount) {

    public static OrderLine of(String number, String amount) {
        return new OrderLine(number, new BigDecimal(amount));
    }
}
