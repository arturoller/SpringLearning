package s10_transactions.tx04_read_only_isolation;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * StockService = sprzedaż ze stanu magazynowego. Pokazuje utraconą aktualizację (lost update) i trzy sposoby obrony.
 * Parametr {@code meanwhile} („w międzyczasie”) to miejsce, w które test wstawia działanie DRUGIEGO użytkownika —
 * dokładnie między odczytem a zapisem. W prawdziwej aplikacji tego parametru nie ma: „w międzyczasie” to po prostu
 * czas, w którym inne żądania pracują równolegle.
 */
@Service
public class StockService {

    private final ProductRepository products;
    private final VersionedProductRepository versionedProducts;
    private final JdbcClient jdbc;

    public StockService(ProductRepository products, VersionedProductRepository versionedProducts, JdbcClient jdbc) {
        this.products = products;
        this.versionedProducts = versionedProducts;
        this.jdbc = jdbc;
    }

    /** Zwykła sprzedaż: odczyt encji, zmiana pola, UPDATE przy commicie (dirty checking). */
    @Transactional
    public void sell(String sku, int pieces) {
        products.findById(sku).orElseThrow().sell(pieces);
    }

    /**
     * Sprzedaż „czytaj → (ktoś inny w międzyczasie) → zapisz”. Hibernate zapisze stan obliczony z NIEAKTUALNEGO odczytu:
     * {@code update products set stock = 6} — sprzedaż drugiego użytkownika przepada (lost update).
     */
    @Transactional
    public void sellWithPause(String sku, int pieces, Runnable meanwhile) {
        Product product = products.findById(sku).orElseThrow();      // odczyt: stock = 7
        meanwhile.run();                                              // drugi użytkownik sprzedaje 1 szt. → w bazie 6
        product.sell(pieces);                                         // u nas 7 - 1 = 6 → UPDATE ... set stock = 6
    }

    /**
     * OBRONA 1: atomowy UPDATE — odczyt, sprawdzenie i zapis w JEDNYM zapytaniu. Baza liczy {@code stock - ?} na
     * aktualnej wartości wiersza, więc nie ma „starego odczytu”, który mógłby nadpisać cudzą zmianę.
     * Zwraca true, gdy sprzedano (był wystarczający stan).
     */
    @Transactional
    public boolean sellAtomically(String sku, int pieces) {
        int rows = jdbc.sql("update products set stock = stock - :pieces where sku = :sku and stock >= :pieces")
                .param("pieces", pieces)
                .param("sku", sku)
                .update();
        return rows == 1;
    }

    /**
     * OBRONA 2: blokowanie optymistyczne ({@code @Version}). Hibernate wyśle
     * {@code update versioned_products set stock=?, version=? where sku=? and version=?} — gdy ktoś w międzyczasie
     * zmienił wiersz (i podbił wersję), UPDATE nie trafi w żaden wiersz → ObjectOptimisticLockingFailureException.
     */
    @Transactional
    public void sellVersioned(String sku, int pieces, Runnable meanwhile) {
        VersionedProduct product = versionedProducts.findById(sku).orElseThrow();
        meanwhile.run();
        product.sell(pieces);
    }

    /** Zwykła sprzedaż produktu z wersją (dla „drugiego użytkownika” i ćwiczeń). */
    @Transactional
    public void sellVersioned(String sku, int pieces) {
        versionedProducts.findById(sku).orElseThrow().sell(pieces);
    }

    /**
     * OBRONA 4: blokowanie pesymistyczne. findForUpdate blokuje wiersz już przy ODCZYCIE — drugi użytkownik, który
     * chce go zmienić, musi czekać do końca naszej transakcji.
     */
    @Transactional
    public void sellWithPessimisticLock(String sku, int pieces, Runnable meanwhile) {
        Product product = products.findForUpdate(sku).orElseThrow();
        meanwhile.run();
        product.sell(pieces);
    }

    /**
     * Transakcja z limitem czasu 1 s, która najpierw czeka na blokadę wiersza trzymaną przez drugiego użytkownika
     * (H2 przerywa takie czekanie dopiero po swoim limicie — domyślnie 2 s), a potem próbuje jeszcze odczytać stan.
     * Złapanie wyjątku służy tylko pokazaniu, co dzieje się z NASTĘPNYM zapytaniem po przekroczeniu terminu.
     */
    @Transactional(timeout = 1)
    public int sellWithTimeout(String sku, int pieces) {
        try {
            sellAtomically(sku, pieces);                    // this.metoda() — zwykłe wywołanie w tej samej transakcji
        } catch (QueryTimeoutException lockWaitTooLong) {
            // „nie udało się — sprawdźmy chociaż stan”…
        }
        return jdbc.sql("select stock from products where sku = ?").param(sku).query(Integer.class).single();
    }

    /**
     * OBRONA 3 (zależna od bazy): wyższy poziom izolacji. H2 w REPEATABLE_READ nie pozwala nadpisać wiersza, który inna
     * transakcja zmieniła i zatwierdziła po naszym odczycie — wycofuje NASZĄ transakcję (błąd 40001).
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void sellWithPauseRepeatableRead(String sku, int pieces, Runnable meanwhile) {
        Product product = products.findById(sku).orElseThrow();
        meanwhile.run();
        product.sell(pieces);
    }
}
