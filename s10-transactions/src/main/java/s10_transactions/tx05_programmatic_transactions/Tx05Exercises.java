package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Ćwiczenia lekcji Tx05ProgrammaticTransactions (opis każdego ćwiczenia — w Javadocu metody). W tych ćwiczeniach
 * NIE używaj {@code @Transactional} — transakcje otwieraj w kodzie (pola transactionTemplate i transactionManager).
 * Dopóki nie rozwiążesz ćwiczeń, metody tylko rzucają UnsupportedOperationException.
 * Mapy cen: sku → nowa cena (testy podają LinkedHashMap, więc kolejność jest stała).
 * Testy: Tx05ProgrammaticTransactionsExercisesTest (uruchamiasz sam ▶).
 */
@Service
public class Tx05Exercises {

    private final ProductRepository products;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;
    private final PlatformTransactionManager transactionManager;

    public Tx05Exercises(ProductRepository products, JdbcClient jdbc, TransactionTemplate transactionTemplate,
                         PlatformTransactionManager transactionManager) {
        this.products = products;
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.transactionManager = transactionManager;
    }

    /**
     * ĆWICZENIE 1 (łatwe): podnieś cenę produktu o {@code percent} procent i zwróć NOWĄ cenę.
     * Nowa cena = cena × (100 + percent) / 100, zaokrąglona do groszy:
     * {@code price.multiply(BigDecimal.valueOf(100 + percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)}.
     * Zmień encję (product.changePrice) w transakcji z transactionTemplate.execute — bez save(). Nieznany sku →
     * NoSuchElementException (findById(...).orElseThrow()).
     * Podpowiedź: sekcja 1 lekcji — execute zwraca to, co zwróci lambda.
     */
    public BigDecimal raisePriceByPercent(String sku, int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): „na sucho” — jaka byłaby suma cen CAŁEGO cennika po obniżce wszystkich cen o
     * {@code percent} procent? Każdą cenę zmień w encji: cena × (100 − percent) / 100, zaokrąglona do groszy
     * (HALF_UP, jak w ćwiczeniu 1), a sumę policz w bazie: {@code select sum(price) from products}. Zwróć sumę.
     * W bazie NIE może zostać żadna zmiana.
     * Podpowiedź: sekcja 2 lekcji — dwie rzeczy: products.flush() przed zapytaniem SQL i status.setRollbackOnly().
     */
    public BigDecimal previewCatalogValueAfterDiscount(int percent) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): import cen PACZKAMI: kolejne {@code chunkSize} pozycji z mapy (w jej kolejności) to jedna
     * transakcja. Zła pozycja (cena ≤ 0 łamie CHECK w bazie przy commicie, nieznany sku → NoSuchElementException)
     * wycofuje całą SWOJĄ paczkę — także dobre ceny z tej paczki — ale import idzie dalej. Zwróć liczbę cen
     * zapisanych w bazie. Cenę zmieniaj przez {@code products.findById(sku).orElseThrow().changePrice(cena)}.
     * Podpowiedź: sekcja 3 lekcji; podziel wpisy mapy na listy po chunkSize (np. new ArrayList<>(map.entrySet())
     * i subList) — każda paczka w osobnym executeWithoutResult, wyjątek łapany POZA nim.
     */
    public int importInChunks(Map<String, BigDecimal> prices, int chunkSize) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zmień wszystkie ceny z mapy w JEDNEJ transakcji („wszystko albo nic”), używając
     * BEZPOŚREDNIO transactionManager (getTransaction / commit / rollback) — bez TransactionTemplate. Wymagania:
     * gdy coś się nie uda, w bazie nie zostaje żadna zmiana, a wołający dostaje PRAWDZIWĄ przyczynę
     * (NoSuchElementException dla nieznanego sku, DataIntegrityViolationException dla ceny ≤ 0 — ten błąd wychodzi
     * dopiero przy commicie). Po metodzie żadna transakcja nie może zostać przypięta do wątku.
     * Podpowiedź: sekcja 6 lekcji — gdzie postawić commit, żeby nie dostać IllegalTransactionStateException?
     */
    public void changePricesManually(Map<String, BigDecimal> prices) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
