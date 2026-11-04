package s10_transactions.tx05_programmatic_transactions;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Sekcja 1 lekcji Tx05ProgrammaticTransactions: {@code TransactionTemplate} (szablon transakcji) — transakcja wokół
 * DOWOLNEGO kawałka kodu (lambdy), a nie całej metody. Bean {@code transactionTemplate} tworzy autokonfiguracja Boota
 * (ustawienia domyślne: PROPAGATION_REQUIRED, ISOLATION_DEFAULT).
 */
@Service
public class OrderImporter {

    private final TransactionTemplate tx;
    private final JdbcClient jdbc;

    public OrderImporter(TransactionTemplate tx, JdbcClient jdbc) {
        this.tx = tx;
        this.jdbc = jdbc;
    }

    /**
     * PO: każda pozycja we WŁASNEJ transakcji. Błąd jednej (wyjątek wylatuje z {@code executeWithoutResult} → rollback
     * tylko tej pozycji) łapiemy POZA transakcją, zapisujemy w raporcie i idziemy dalej. Ta metoda NIE ma
     * {@code @Transactional} — inaczej wszystkie pozycje dołączyłyby do jednej transakcji (REQUIRED).
     */
    public ImportReport importEach(List<OrderLine> lines) {
        List<String> imported = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (OrderLine line : lines) {
            try {
                tx.executeWithoutResult(status -> OrderSql.insertOrder(jdbc, line, "IMPORTED")); // without result = bez wyniku
                imported.add(line.number());
            } catch (DataAccessException e) { // błąd bazy (DuplicateKeyException, DataIntegrityViolationException…)
                failed.add(line.number());
            }
        }
        return new ImportReport(imported, failed);
    }

    /**
     * PUŁAPKA: jak {@link #importEach}, ale z {@code @Transactional} na metodzie. Szablony w pętli DOŁĄCZAJĄ (REQUIRED)
     * do jej transakcji; błąd pozycji oznacza całość jako rollback-only, a commit na końcu kończy się
     * UnexpectedRollbackException — nic nie zostaje zapisane, choć wyjątki „obsłużyliśmy”.
     */
    @Transactional
    public ImportReport importEachInsideOneTransaction(List<OrderLine> lines) {
        return importEach(lines); // this.importEach — zwykłe wywołanie; szablon w środku i tak widzi transakcję
    }

    /** PRZED: jedna transakcja na całą partię — jedna zła pozycja wycofuje wszystkie poprzednie. */
    @Transactional
    public void importAllOrNothing(List<OrderLine> lines) {
        lines.forEach(line -> OrderSql.insertOrder(jdbc, line, "IMPORTED"));
    }

    /**
     * Tylko CZĘŚĆ metody w transakcji: przygotowanie (np. walidacja, liczenie, wywołanie innej usługi) idzie bez
     * transakcji, a {@code execute} otwiera ją dopiero na zapis i zwraca wynik lambdy.
     */
    public PartialView saveWithPreparation(OrderLine line) {
        TxState duringPreparation = TxState.now();
        OrderLine prepared = new OrderLine(line.number().trim().toUpperCase(), line.amount()); // „przygotowanie”
        TxState duringSave = tx.execute(status -> { // execute = wykonaj (i zwróć wynik lambdy)
            OrderSql.insertOrder(jdbc, prepared, "NEW");
            return TxState.now();
        });
        return new PartialView(duringPreparation, duringSave);
    }

    /** Wynik importu: numery zapisane i odrzucone (w kolejności z wejścia). */
    public record ImportReport(List<String> imported, List<String> failed) {
    }

    /** Stan transakcji w czasie przygotowania i w czasie zapisu. */
    public record PartialView(TxState duringPreparation, TxState duringSave) {
    }
}
