package s10_transactions.tx01_transactional;

import java.math.BigDecimal;

/**
 * Za mało środków (insufficient funds = niewystarczające środki). Wyjątek SPRAWDZANY (checked — dziedziczy po
 * {@code Exception}): kompilator wymusza {@code throws} albo {@code catch}. PUŁAPKA z sekcji 3 lekcji: domyślnie
 * taki wyjątek NIE wycofuje transakcji — zmiany sprzed wyjątku zostają ZATWIERDZONE.
 */
public class InsufficientFundsException extends Exception {

    public InsufficientFundsException(Long walletId, BigDecimal requested, BigDecimal balance) {
        super("Portfel " + walletId + ": żądano " + requested + ", saldo " + balance);
    }
}
