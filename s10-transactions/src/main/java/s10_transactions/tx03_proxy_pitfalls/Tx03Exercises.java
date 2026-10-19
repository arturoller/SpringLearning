package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * Ćwiczenia lekcji Tx03ProxyPitfalls (opis każdego ćwiczenia — w Javadocu metody). Klasa jest beanem, żeby Spring
 * mógł owinąć ją proxy transakcyjnym, gdy dopiszesz {@code @Transactional}. Dopóki nie rozwiążesz ćwiczeń, metody
 * tylko rzucają UnsupportedOperationException — aplikacja lekcji i domyślne testy działają normalnie.
 * Mapy: sku → liczba sztuk (testy podają LinkedHashMap, więc kolejność jest stała).
 * Testy: Tx03ProxyPitfallsExercisesTest (uruchamiasz sam ▶).
 */
@Service
public class Tx03Exercises {

    /** Największa dopuszczalna liczba sztuk jednej pozycji w magazynie (ćwiczenie 3). */
    public static final int MAX_QUANTITY = 1000;

    private final StockItemRepository stock;
    private final TransactionTemplate transactionTemplate;

    public Tx03Exercises(StockItemRepository stock, TransactionTemplate transactionTemplate) {
        this.stock = stock;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * ĆWICZENIE 1 (łatwe): przyjmij wszystkie dostawy z mapy: dla każdej pozycji
     * {@code stock.findById(sku).orElseThrow().add(sztuki)} — BEZ save(). Zasada „wszystkie albo żadna”: gdy któregoś
     * sku nie ma (orElseThrow rzuca NoSuchElementException), w bazie nie może zostać żadna zmiana — także dostaw
     * przyjętych wcześniej w pętli.
     * Podpowiedź: jedna adnotacja — ale nad TĄ metodą (publiczną, wołaną z zewnątrz), a nie nad prywatną metodą
     * pomocniczą. Dlaczego? (sekcje 1 i 2 lekcji)
     */
    public void restockAll(Map<String, Integer> deliveries) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zarezerwuj wszystkie pozycje ({@code findById(sku).orElseThrow().take(sztuki)}).
     * Gdy którejś pozycji zabraknie (take rzuca IllegalStateException), metoda ma ZWRÓCIĆ false — bez wyjątku —
     * a w bazie nie może zostać żadna zmiana. Gdy wszystko się uda — zwróć true (zmiany zapisane).
     * Podpowiedź: sekcja 3 lekcji — jak wycofać transakcję, nie wypuszczając wyjątku z metody?
     */
    public boolean reserveAllOrNothing(Map<String, Integer> items) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): przyjmij KAŻDĄ dostawę w OSOBNEJ transakcji: pobierz pozycję, dodaj sztuki (add),
     * a POTEM sprawdź limit — gdy stan przekracza {@link #MAX_QUANTITY}, rzuć IllegalStateException. Nieudana dostawa
     * (limit albo nieznany sku) ma zostać wycofana, a udane — zapisane. Zwróć listę sku nieudanych dostaw
     * (w kolejności z mapy). Metoda NIE może być jedną wspólną transakcją (wtedy jeden błąd cofnąłby wszystko).
     * PUŁAPKA: osobna metoda {@code @Transactional} w TEJ klasie wołana jako this.metoda() nie otworzy transakcji.
     * Podpowiedź: pole transactionTemplate (sekcja 5 lekcji, naprawa 3) — execute/executeWithoutResult w pętli,
     * a wyjątek złap DOPIERO poza szablonem (dlaczego nie w środku? — sekcja 3).
     */
    public List<String> restockEachSeparately(Map<String, Integer> deliveries) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
