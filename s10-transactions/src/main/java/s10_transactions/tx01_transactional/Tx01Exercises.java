package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Ćwiczenia lekcji Tx01Transactional (opis każdego ćwiczenia — w Javadocu metody). Klasa jest beanem, żeby Spring
 * mógł owinąć ją proxy transakcyjnym, gdy dopiszesz {@code @Transactional}. Dopóki nie rozwiążesz ćwiczeń, metody
 * tylko rzucają UnsupportedOperationException — aplikacja lekcji i domyślne testy działają normalnie.
 * Testy: Tx01TransactionalExercisesTest (uruchamiasz sam ▶).
 */
@Service
public class Tx01Exercises {

    private final AccountRepository accounts;

    public Tx01Exercises(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /**
     * ĆWICZENIE 1 (łatwe): przelej {@code amount} groszy z konta {@code from} na {@code to}. Po przelewie, jeśli kwota
     * przekracza 50000 groszy (limit banku), rzuć {@code new PaymentGatewayException("Przekroczony limit")}.
     * Wtedy przelew ma zostać WYCOFANY (oba salda bez zmian), choć wyjątek jest kontrolowany.
     * Podpowiedź: {@code @Transactional(rollbackFor = ...)} nad metodą; do kont użyj pola accounts (findById + debit/credit).
     */
    public void transferWithLimit(String from, String to, long amount) throws PaymentGatewayException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): obciąż konto {@code number} kwotą {@code amount}, a potem „wydrukuj paragon”, który ZAWSZE
     * się psuje: {@code throw new ReceiptPrinterException("Drukarka offline")}. Wyjątek ma polecieć do wołającego,
     * ale obciążenie konta ma ZOSTAĆ w bazie.
     * Podpowiedź: domyślnie RuntimeException wycofuje transakcję — który atrybut {@code @Transactional} to zmienia?
     */
    public void chargeWithBrokenPrinter(String number, long amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z konta {@code from} zapłać po kolei wszystkie płatności z mapy (numer konta → kwota)
     * i zwróć sumę zapłaconych groszy. Zasada „wszystko albo nic”: gdy któregoś konta docelowego nie ma
     * (AccountNotFoundException) albo zabraknie środków (IllegalStateException), w bazie NIE może zostać żadna zmiana
     * — także z płatności wykonanych wcześniej w tej pętli.
     * Podpowiedź: jedna transakcja na całą metodę; nie łap wyjątków w środku (dlaczego? — pomyśl o commicie).
     */
    public long payAll(String from, Map<String, Long> payments) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
