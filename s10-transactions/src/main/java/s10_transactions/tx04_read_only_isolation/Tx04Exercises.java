package s10_transactions.tx04_read_only_isolation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ćwiczenia lekcji Tx04ReadOnlyIsolation (opis każdego ćwiczenia — w Javadocu metody). Klasa jest beanem, żeby Spring
 * mógł owinąć ją proxy transakcyjnym, gdy dopiszesz {@code @Transactional}. Dopóki nie rozwiążesz ćwiczeń, metody
 * tylko rzucają UnsupportedOperationException — aplikacja lekcji i domyślne testy działają normalnie.
 * Parametr {@code meanwhile} — jak w lekcji: tu test wstawia działanie drugiego użytkownika.
 * Testy: Tx04ReadOnlyIsolationExercisesTest (uruchamiasz sam ▶).
 */
@Service
public class Tx04Exercises {

    private final JdbcClient jdbc;
    private final ProductRepository products;
    private final StockService stockService;

    public Tx04Exercises(JdbcClient jdbc, ProductRepository products, StockService stockService) {
        this.jdbc = jdbc;
        this.products = products;
        this.stockService = stockService;
    }

    /**
     * ĆWICZENIE 1 (łatwe): odczytaj stan produktu czystym SQL
     * ({@code jdbc.sql("select stock from products where sku = ?")...}), wywołaj {@code meanwhile.run()}, odczytaj
     * stan drugi raz i zwróć {@code List.of(pierwszy, drugi)}. Oba odczyty mają być RÓWNE, nawet gdy w meanwhile
     * drugi użytkownik zatwierdzi zmianę. Nie używaj findById — chodzi o izolację BAZY, nie o pamięć Hibernate.
     * Podpowiedź: sekcja 3 lekcji — jeden atrybut {@code @Transactional}.
     */
    public List<Integer> readStockTwiceStable(String sku, Runnable meanwhile) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): sprzedaj {@code pieces} sztuk produktu w tej kolejności: najpierw sprawdź, że produkt
     * istnieje ({@code products.findById(sku).orElseThrow()}), potem wywołaj {@code meanwhile.run()} (drugi użytkownik
     * sprzedaje „w tej samej chwili”), na końcu zapisz sprzedaż. Wymagania: sprzedaż drugiego użytkownika NIE może
     * zginąć, a stan nie może spaść poniżej zera — gdy towaru jest za mało, zwróć false i niczego nie zmieniaj;
     * po udanej sprzedaży zwróć true.
     * Podpowiedź: sekcja 5 lekcji, obrona 1. Dlaczego product.sell(pieces) na encji z pierwszego kroku NIE zadziała?
     */
    public boolean sellWithPauseSafely(String sku, int pieces, Runnable meanwhile) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): sprzedaj produkt z wersją ({@code @Version}). Pierwsza próba:
     * {@code stockService.sellVersioned(sku, pieces, firstAttemptMeanwhile)} — test może tam wstawić konflikt.
     * Gdy poleci ObjectOptimisticLockingFailureException, spróbuj JESZCZE RAZ: {@code stockService.sellVersioned(sku,
     * pieces)}. Zwróć liczbę prób (1 albo 2). Każda próba musi być OSOBNĄ transakcją.
     * PUŁAPKA: co się stanie, jeśli dopiszesz {@code @Transactional} nad TĄ metodą? (Kiedy Hibernate wysyła UPDATE
     * z warunkiem na wersję i kiedy leci wyjątek — sekcja 5 lekcji.)
     */
    public int sellVersionedWithRetry(String sku, int pieces, Runnable firstAttemptMeanwhile) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
