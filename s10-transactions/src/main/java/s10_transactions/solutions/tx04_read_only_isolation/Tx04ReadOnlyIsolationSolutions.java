package s10_transactions.solutions.tx04_read_only_isolation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import s10_transactions.tx04_read_only_isolation.ProductRepository;
import s10_transactions.tx04_read_only_isolation.StockService;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Tx04ReadOnlyIsolation — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu lekcji — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)}.
 */
@Service
public class Tx04ReadOnlyIsolationSolutions {

    private final JdbcClient jdbc;
    private final ProductRepository products;
    private final StockService stockService;

    public Tx04ReadOnlyIsolationSolutions(JdbcClient jdbc, ProductRepository products, StockService stockService) {
        this.jdbc = jdbc;
        this.products = products;
        this.stockService = stockService;
    }

    private int stock(String sku) {
        return jdbc.sql("select stock from products where sku = ?").param(sku).query(Integer.class).single();
    }

    /** REPEATABLE_READ: przez całą transakcję widzimy ten sam stan wiersza. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Integer> readStockTwiceStable(String sku, Runnable meanwhile) {
        int first = stock(sku);
        meanwhile.run();
        return List.of(first, stock(sku));
    }

    /**
     * Zapis atomowym UPDATE: baza odejmuje od AKTUALNEJ wartości (już po sprzedaży drugiego użytkownika) i sama
     * pilnuje warunku {@code stock >= pieces}. Encja z pierwszego kroku ma stary stan — jej zmiana nadpisałaby cudzą
     * sprzedaż (lost update).
     */
    @Transactional
    public boolean sellWithPauseSafely(String sku, int pieces, Runnable meanwhile) {
        products.findById(sku).orElseThrow();
        meanwhile.run();
        int rows = jdbc.sql("update products set stock = stock - :pieces where sku = :sku and stock >= :pieces")
                .param("pieces", pieces)
                .param("sku", sku)
                .update();
        return rows == 1;
    }

    /**
     * BEZ @Transactional: każda próba to osobne wywołanie przez proxy StockService = osobna transakcja. Konflikt
     * wersji wychodzi przy commicie PIERWSZEJ próby — wtedy druga próba czyta świeży wiersz (z nową wersją).
     * Z @Transactional nad tą metodą obie próby byłyby jedną transakcją: flush (i wyjątek) dopiero na samym końcu,
     * już po „udanej” drugiej próbie.
     */
    public int sellVersionedWithRetry(String sku, int pieces, Runnable firstAttemptMeanwhile) {
        try {
            stockService.sellVersioned(sku, pieces, firstAttemptMeanwhile);
            return 1;
        } catch (ObjectOptimisticLockingFailureException conflict) {
            stockService.sellVersioned(sku, pieces);
            return 2;
        }
    }
}
