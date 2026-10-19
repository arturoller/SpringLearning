package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Map;

/**
 * ReservationService = rezerwacja towaru pod zamówienie (zdjęcie sztuk ze stanu). Pokazuje pułapki z WYJĄTKAMI:
 * proxy decyduje o commit/rollback WYŁĄCZNIE na podstawie tego, co WYLATUJE z metody. Wyjątek złapany w środku albo
 * zamieniony na kontrolowany = dla proxy „wszystko w porządku” = COMMIT.
 * Mapa pozycji: sku → liczba sztuk (LinkedHashMap w testach, żeby kolejność była stała).
 */
@Service
public class ReservationService {

    private final StockItemRepository stock;

    public ReservationService(StockItemRepository stock) {
        this.stock = stock;
    }

    private void take(String sku, int pieces) {
        stock.findById(sku).orElseThrow().take(pieces);
    }

    /**
     * PUŁAPKA: błędy „tylko logujemy” (sekcja 3a). Wyjątek nie wychodzi z metody, więc proxy widzi normalny powrót
     * → COMMIT: udane pozycje zostają zdjęte, a nieudane nie. Zwraca liczbę zarezerwowanych pozycji.
     */
    @Transactional
    public int reserveIgnoringErrors(Map<String, Integer> items) {
        int reserved = 0;
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            try {
                take(item.getKey(), item.getValue());
                reserved++;
            } catch (IllegalStateException notEnough) {
                // „tylko logujemy” — i właśnie zatwierdzamy część zamówienia
            }
        }
        return reserved;
    }

    /**
     * PUŁAPKA: „złap niekontrolowany, rzuć kontrolowany” (sekcja 3b). Wyjątek wylatuje (wołający widzi błąd), ale jest
     * KONTROLOWANY → domyślna reguła: COMMIT. Pozycje sprzed błędu zostają zdjęte.
     */
    @Transactional
    public void reserveOrThrowChecked(Map<String, Integer> items) throws ReservationException {
        try {
            items.forEach(this::take);
        } catch (IllegalStateException notEnough) {
            throw new ReservationException("Rezerwacja nieudana: " + notEnough.getMessage(), notEnough);
        }
    }

    /** Naprawa sekcji 3b: ten sam kod z {@code rollbackFor} — wyjątek kontrolowany też wycofuje transakcję. */
    @Transactional(rollbackFor = ReservationException.class)
    public void reserveOrThrowCheckedWithRollback(Map<String, Integer> items) throws ReservationException {
        try {
            items.forEach(this::take);
        } catch (IllegalStateException notEnough) {
            throw new ReservationException("Rezerwacja nieudana: " + notEnough.getMessage(), notEnough);
        }
    }

    /**
     * Naprawa sekcji 3a, gdy chcesz ZWRÓCIĆ wynik zamiast rzucać wyjątek: oznacz bieżącą transakcję „tylko do
     * wycofania”. {@code TransactionAspectSupport.currentTransactionStatus()} (aspect support = wsparcie aspektu)
     * zwraca status transakcji otwartej przez proxy {@code @Transactional}; setRollbackOnly() = „przy wyjściu wycofaj”.
     * Metoda kończy się normalnie (zwraca false), a proxy robi ROLLBACK zamiast COMMIT.
     */
    @Transactional
    public boolean reserveOrReport(Map<String, Integer> items) {
        try {
            items.forEach(this::take);
            return true;
        } catch (IllegalStateException notEnough) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
