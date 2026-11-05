package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;

/**
 * Przelew między portfelami (transfer = przelew) — sekcje 1 i 4 lekcji Tx01Transactional.
 * Wszystkie metody wykonują TEN SAM kod ({@link #moveMoney}); różnią się tylko adnotacją i obsługą wyjątku.
 * Przelew do nieistniejącego portfela (np. 99) to nasza „awaria w połowie”: pieniądze są już pobrane z pierwszego
 * portfela, gdy okazuje się, że drugiego nie ma.
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class TransferService {

    private final WalletRepository wallets;

    public TransferService(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /**
     * Sekcja 1, PRZED: bez {@code @Transactional}. Każde {@code save} ma własną transakcję repozytorium i zatwierdza
     * zmianę od razu — po wyjątku w połowie pieniądze z portfela {@code fromId} ZNIKAJĄ (test).
     */
    public void transferWithoutTransaction(Long fromId, Long toId, BigDecimal amount) {
        moveMoney(fromId, toId, amount);
    }

    /**
     * Sekcja 1, PO: {@code @Transactional} (transactional = transakcyjny). Oba UPDATE-y są w JEDNEJ transakcji:
     * wyjątek wychodzi z metody przez proxy → rollback (wycofanie) obu zmian (test).
     */
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        moveMoney(fromId, toId, amount);
    }

    /**
     * Sekcja 4, PUŁAPKA: wyjątek złapany W ŚRODKU metody nie dociera do proxy, więc proxy uznaje, że wszystko poszło
     * dobrze, i robi COMMIT — wypłata z pierwszego portfela zostaje zapisana, wpłaty nie ma (test).
     */
    @Transactional
    public boolean transferSwallowingError(Long fromId, Long toId, BigDecimal amount) {
        try {
            moveMoney(fromId, toId, amount);
            return true;
        } catch (WalletNotFoundException e) {
            return false; // „połknięty” wyjątek — proxy nic o nim nie wie
        }
    }

    /**
     * Sekcja 4, PO: łapiemy wyjątek, ale RĘCZNIE oznaczamy transakcję „tylko do wycofania” (rollback-only).
     * {@code TransactionAspectSupport.currentTransactionStatus()} (aspect support = wsparcie aspektu) zwraca status
     * transakcji otwartej przez proxy; {@code setRollbackOnly()} każe proxy zrobić rollback zamiast commitu —
     * bez rzucania wyjątku do wywołującego (test).
     */
    @Transactional
    public boolean transferOrRollbackQuietly(Long fromId, Long toId, BigDecimal amount) {
        try {
            moveMoney(fromId, toId, amount);
            return true;
        } catch (WalletNotFoundException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /** Wspólny kod przelewu: najpierw wypłata (i zapis), potem wpłata (i zapis). */
    private void moveMoney(Long fromId, Long toId, BigDecimal amount) {
        Wallet from = wallets.findById(fromId).orElseThrow(() -> new WalletNotFoundException(fromId));
        from.withdraw(amount);
        // W transakcji save() nie jest potrzebne (dirty checking — wykrywanie zmian — zapisze encję przy commicie),
        // ale BEZ transakcji to właśnie save() wysyła UPDATE. Zostawiamy je, żeby obie wersje miały identyczny kod.
        wallets.save(from);
        Wallet to = wallets.findById(toId).orElseThrow(() -> new WalletNotFoundException(toId)); // ← awaria w połowie
        to.deposit(amount);
        wallets.save(to);
    }
}
