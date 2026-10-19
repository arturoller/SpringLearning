package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * ReportService = raporty magazynu, czyli metody, które z założenia TYLKO CZYTAJĄ. Pokazuje, co naprawdę robi
 * {@code @Transactional(readOnly = true)} (read only = tylko do odczytu) z Hibernate — i czego NIE robi.
 */
@Service
public class ReportService {

    private final ProductRepository products;
    private final StockService stockService;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    public ReportService(ProductRepository products, StockService stockService, EntityManager entityManager,
                         JdbcTemplate jdbcTemplate) {
        this.products = products;
        this.stockService = stockService;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Co widać „pod maską” w transakcji: flaga Springa, tryb flush sesji Hibernate, domyślny tryb tylko do odczytu. */
    public record TransactionInsight(boolean springReadOnly, FlushMode flushMode, boolean sessionDefaultReadOnly) {
    }

    /** Podgląd w transakcji tylko do odczytu. */
    @Transactional(readOnly = true)
    public TransactionInsight insideReadOnly() {
        return insight();
    }

    /** Ten sam podgląd w zwykłej transakcji (do porównania). */
    @Transactional
    public TransactionInsight insideReadWrite() {
        return insight();
    }

    private TransactionInsight insight() {
        Session session = entityManager.unwrap(Session.class);        // unwrap = „odpakuj” — sesja Hibernate pod JPA
        return new TransactionInsight(TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                session.getHibernateFlushMode(), session.isDefaultReadOnly());
    }

    /**
     * Zmiana encji w transakcji tylko do odczytu. Obiekt w pamięci się zmienia (zwracamy nowy stan), ale do bazy NIC
     * nie trafia: encja jest wczytana jako tylko do odczytu, a sesja nie robi flush przy commicie.
     */
    @Transactional(readOnly = true)
    public int sellInReadOnlyTransaction(String sku, int pieces) {
        Product product = products.findById(sku).orElseThrow();
        product.sell(pieces);
        return product.getStock();
    }

    /** Jak wyżej, ale z jawnym save() — też NIE trafia do bazy (save robi merge, a flush przy commicie nie następuje). */
    @Transactional(readOnly = true)
    public void sellAndSaveInReadOnlyTransaction(String sku, int pieces) {
        Product product = products.findById(sku).orElseThrow();
        product.sell(pieces);
        products.save(product);
    }

    /** Ręczny SQL UPDATE w transakcji tylko do odczytu — readOnly to nie zakaz zapisu (zależy od bazy). */
    @Transactional(readOnly = true)
    public int sqlUpdateInReadOnlyTransaction(String sku, int stock) {
        return jdbcTemplate.update("update products set stock = ? where sku = ?", stock, sku);
    }

    /**
     * PUŁAPKA: „raport” tylko do odczytu woła metodę ZAPISUJĄCĄ innego beana. StockService.sell ma zwykłe
     * {@code @Transactional} (REQUIRED), więc DOŁĄCZA do transakcji raportu — a ta jest tylko do odczytu.
     * Sprzedaż po cichu przepada: bez wyjątku, bez UPDATE.
     */
    @Transactional(readOnly = true)
    public void reportThatSells(String sku, int pieces) {
        stockService.sell(sku, pieces);
    }
}
