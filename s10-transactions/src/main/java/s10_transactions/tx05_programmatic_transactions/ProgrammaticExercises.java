package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * ĆWICZENIA lekcji Tx05ProgrammaticTransactions (testy: Tx05ProgrammaticTransactionsExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem. Do dyspozycji: wspólny szablon {@code tx}, menedżer
 * {@code transactionManager} (do własnych szablonów), {@code jdbc} i {@code mail}. SQL zamówienia:
 * {@code INSERT INTO customer_order (order_number, amount, status) VALUES (?, ?, ?)} (status np. "NEW").
 * Dane przed każdym testem: brak zamówień, brak wpisów audytu, brak wysłanych e-maili.
 */
@Service
public class ProgrammaticExercises {

    private final TransactionTemplate tx;
    private final PlatformTransactionManager transactionManager;
    private final JdbcClient jdbc;
    private final FakeEmailSender mail;

    public ProgrammaticExercises(TransactionTemplate tx, PlatformTransactionManager transactionManager, JdbcClient jdbc,
                                 FakeEmailSender mail) {
        this.tx = tx;
        this.transactionManager = transactionManager;
        this.jdbc = jdbc;
        this.mail = mail;
    }

    /**
     * ĆWICZENIE 1 (łatwe): „próba na sucho” — w transakcji wstaw zamówienie, policz zamówienia widoczne W ŚRODKU
     * transakcji ({@code SELECT COUNT(*) FROM customer_order}) i zwróć tę liczbę, ale niczego nie zatwierdzaj.
     * Bez rzucania wyjątku. Podpowiedź: sekcja 2 — {@code tx.execute(status -> …)} i jedna metoda {@code status}.
     */
    public long dryRunCount(OrderLine line) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zaimportuj pozycje — każdą w osobnej transakcji. Zwróć numery pozycji, których nie dało się
     * zapisać (błąd bazy: {@code DataAccessException} z org.springframework.dao), w kolejności z wejścia. Poprawne
     * pozycje mają zostać w bazie. Podpowiedź: sekcja 1 — i czy ta metoda może mieć {@code @Transactional}?
     */
    public List<String> importReturningFailures(List<OrderLine> lines) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zwróć {@code new ReportSnapshot(TxState.now(), poziom)} z wnętrza transakcji, która jest
     * TYLKO DO ODCZYTU, ma poziom REPEATABLE READ i nazwę {@code "exerciseReport"}; poziom odczytaj z bazy:
     * {@code SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()}.
     * Wspólnego beana {@code tx} NIE wolno zmieniać (test to sprawdzi).
     * Podpowiedź: sekcja 2 — {@code new TransactionTemplate(transactionManager)} i settery.
     */
    public ReportSnapshot readOnlyReport() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zapisz WSZYSTKIE zamówienia w JEDNEJ transakcji (błąd jednego = żadne nie zostaje)
     * i dla każdego wyślij {@code mail.send("klient@example.com", "Przyjęliśmy zamówienie " + numer)} — ale dopiero
     * wtedy, gdy transakcja się ZATWIERDZI. Po błędzie nie może wyjść ani jeden e-mail, a wyjątek ma dotrzeć do
     * wywołującego. Podpowiedź: sekcja 4 — {@code TransactionSynchronizationManager.registerSynchronization}.
     */
    public void placeOrdersAndNotify(List<OrderLine> lines) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /** Wynik ćwiczenia 3. */
    public record ReportSnapshot(TxState state, String isolationLevel) {
    }
}
