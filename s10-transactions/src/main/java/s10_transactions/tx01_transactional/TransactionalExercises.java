package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * ĆWICZENIA lekcji Tx01Transactional (testy: Tx01TransactionalExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem. Testy działają na pełnej aplikacji BEZ transakcji testu, więc widzą
 * prawdziwy commit i rollback. Dane przed każdym testem: portfel 1 = 500.00, portfel 2 = 100.00.
 */
@Service
public class TransactionalExercises {

    private final WalletRepository wallets;

    public TransactionalExercises(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /**
     * ĆWICZENIE 1 (łatwe): przenieś CAŁE saldo portfela {@code fromId} do portfela {@code toId}.
     * Gdy któregoś portfela nie ma — rzuć {@link WalletNotFoundException} i NIC nie zmieniaj (także gdy brakuje
     * dopiero drugiego portfela, a z pierwszego już „wypłaciłeś”).
     * Podpowiedź: sekcja 1 — jedna adnotacja na metodzie; kolejność: znajdź {@code from}, wypłać, znajdź {@code to}, wpłać.
     */
    public void transferAll(Long fromId, Long toId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zapłać fakturę: pobierz opłatę {@link FeeService#FEE}, a potem kwotę {@code amount}.
     * Gdy po opłacie saldo jest mniejsze niż {@code amount} — rzuć {@link InsufficientFundsException} (wyjątek
     * SPRAWDZANY!) i NIE pobieraj też opłaty (saldo bez zmian).
     * Podpowiedź: sekcja 3 — {@code @Transactional(rollbackFor = ...)}; bez tego opłata zostanie zatwierdzona.
     */
    public void payInvoice(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wypłać {@code amount} z KAŻDEGO portfela z listy (po kolei). Jeśli w którymkolwiek
     * brakuje środków — zwróć {@code false} BEZ rzucania wyjątku, a wszystkie wcześniejsze wypłaty z tej listy mają
     * zostać wycofane. Gdy wszystko się uda — {@code true}.
     * Podpowiedź: sekcja 4 — {@code TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()}; pamiętaj,
     * że zmienione encje zarządzane Hibernate zapisze przy commicie nawet bez {@code save()}.
     */
    public boolean withdrawFromAll(List<Long> walletIds, BigDecimal amount) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
