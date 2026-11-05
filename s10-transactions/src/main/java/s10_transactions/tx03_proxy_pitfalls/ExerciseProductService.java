package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * ĆWICZENIA 2 i 3 lekcji Tx03ProxyPitfalls (testy: Tx03ProxyPitfallsExercisesTest — uruchamiasz sam ▶).
 * Metody pomocnicze {@link #renameOne}, {@link #renameOneInNewTransaction} i {@link #renameAndReportState} są gotowe —
 * nie zmieniaj ich.
 * Dane przed każdym testem: 1 Klawiatura, 2 Mysz, 3 Monitor.
 */
@Service
public class ExerciseProductService {

    private final JdbcClient jdbc;
    private final ObjectProvider<ExerciseProductService> self; // dostawca „samego siebie” — czyli proxy tego beana

    public ExerciseProductService(JdbcClient jdbc, ObjectProvider<ExerciseProductService> self) {
        this.jdbc = jdbc;
        this.self = self;
    }

    /** Zmiana nazwy jednego produktu; pusta nazwa → IllegalArgumentException (przed jakimkolwiek UPDATE). */
    @Transactional
    public void renameOne(Long productId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Pusta nazwa produktu " + productId);
        }
        jdbc.sql("UPDATE product SET name = ? WHERE id = ?").params(newName, productId).update();
    }

    /** Jak renameOne, ale ZAWSZE w nowej transakcji; zwraca nazwę transakcji, w której zmieniono nazwę. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String renameOneInNewTransaction(Long productId, String newName) {
        renameOne(productId, newName); // this — to celowo zwykłe wywołanie: jesteśmy już w transakcji
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }

    /** Do ćwiczenia 4: renameOne w transakcji tej metody + stan transakcji odczytany w trakcie (w wątku wywołującego). */
    @Transactional
    public TxState renameAndReportState(Long productId, String newName) {
        renameOne(productId, newName);
        return TxState.now();
    }

    /**
     * ĆWICZENIE 2 (średnie): zmień nazwy wszystkich produktów z listy — WSZYSTKIE albo ŻADNA. Gdy jedna nazwa jest
     * pusta, {@code renameOne} rzuci IllegalArgumentException; niech wyjątek wyleci, a wcześniejsze zmiany z listy
     * mają zostać wycofane.
     * Podpowiedź: sekcja 2 — pętla z {@code renameOne(...)} (czyli this.renameOne) NIE otwiera transakcji. Gdzie musi
     * być adnotacja, żeby cała pętla była jedną transakcją?
     */
    public void renameAll(List<Rename> renames) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zmień nazwy KAŻDEGO produktu w OSOBNEJ, nowej transakcji przez
     * {@link #renameOneInNewTransaction}; pozycje z pustą nazwą pomiń (złap IllegalArgumentException). Zwróć listę nazw
     * transakcji zwróconych przez udane zmiany. Każda ma brzmieć
     * {@code "s10_transactions.tx03_proxy_pitfalls.ExerciseProductService.renameOneInNewTransaction"}.
     * Podpowiedź: sekcja 2 — wywołanie przez {@code this} da nazwę transakcji TEJ metody; użyj pola {@code self}
     * ({@code self.getObject()}).
     */
    @Transactional
    public List<String> renameEachSeparately(List<Rename> renames) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /** Jedna zmiana nazwy: id produktu i nowa nazwa. */
    public record Rename(Long productId, String newName) {
    }
}
