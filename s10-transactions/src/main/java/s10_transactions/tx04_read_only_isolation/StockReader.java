package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * StockReader = odczyty stanów magazynowych na różnych poziomach izolacji (isolation = izolacja, czyli „ile widzę
 * z cudzych, równoległych transakcji”). Czytamy czystym SQL (JdbcClient — dzieli połączenie z transakcją JPA), żeby
 * widzieć to, co zwraca BAZA, a nie pamięć podręczna Hibernate. Parametr {@code meanwhile} („w międzyczasie”) to
 * miejsce na działanie drugiego użytkownika między dwoma odczytami.
 */
@Service
public class StockReader {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository products;
    private final EntityManager entityManager;

    public StockReader(JdbcClient jdbc, JdbcTemplate jdbcTemplate, ProductRepository products,
                       EntityManager entityManager) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
        this.products = products;
        this.entityManager = entityManager;
    }

    private int stock(String sku) {
        return jdbc.sql("select stock from products where sku = ?").param(sku).query(Integer.class).single();
    }

    private long count(String category) {
        return jdbc.sql("select count(*) from products where category = ?").param(category).query(Long.class).single();
    }

    // --- brudny odczyt (dirty read) ---------------------------------------------------------------------------

    /** READ_UNCOMMITTED (odczyt niezatwierdzonych): widać NIEZATWIERDZONE zmiany innych transakcji. */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public int readStockReadUncommitted(String sku) {
        return stock(sku);
    }

    /** READ_COMMITTED (odczyt zatwierdzonych): widać tylko to, co inni już zatwierdzili. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int readStockReadCommitted(String sku) {
        return stock(sku);
    }

    // --- niepowtarzalny odczyt (non-repeatable read) ----------------------------------------------------------

    /** Dwa odczyty tego samego wiersza w READ_COMMITTED: [przed, po] zmianie drugiego użytkownika. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Integer> readStockTwiceReadCommitted(String sku, Runnable meanwhile) {
        int first = stock(sku);
        meanwhile.run();
        return List.of(first, stock(sku));
    }

    /** Dwa odczyty tego samego wiersza w REPEATABLE_READ (powtarzalny odczyt). */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Integer> readStockTwiceRepeatableRead(String sku, Runnable meanwhile) {
        int first = stock(sku);
        meanwhile.run();
        return List.of(first, stock(sku));
    }

    // --- fantomy (phantom read) -----------------------------------------------------------------------------

    /** Dwa razy „ile produktów w kategorii?” w READ_COMMITTED. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Long> countTwiceReadCommitted(String category, Runnable meanwhile) {
        long first = count(category);
        meanwhile.run();
        return List.of(first, count(category));
    }

    /** Dwa razy „ile produktów w kategorii?” w REPEATABLE_READ. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Long> countTwiceRepeatableRead(String category, Runnable meanwhile) {
        long first = count(category);
        meanwhile.run();
        return List.of(first, count(category));
    }

    // --- pamięć Hibernate a izolacja --------------------------------------------------------------------------

    /**
     * READ_COMMITTED, ale czytamy ENCJĘ dwa razy: [stan encji przy 1. odczycie, stan encji przy 2. odczycie,
     * stan w bazie wg SQL, stan encji po refresh]. Drugi findById nie pyta bazy — zwraca TEN SAM obiekt z kontekstu
     * utrwalania (persistence context), ze starym stanem. Dopiero refresh (odśwież) czyta wiersz z bazy na nowo.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Integer> readEntityTwice(String sku, Runnable meanwhile) {
        Product first = products.findById(sku).orElseThrow();
        int firstStock = first.getStock();
        meanwhile.run();
        Product second = products.findById(sku).orElseThrow();
        if (second != first) {
            throw new IllegalStateException("Spodziewany ten sam obiekt z kontekstu utrwalania");
        }
        int secondStock = second.getStock();
        int sqlStock = stock(sku);
        entityManager.refresh(second);
        return List.of(firstStock, secondStock, sqlStock, second.getStock());
    }

    // --- jaki poziom izolacji ma NAPRAWDĘ połączenie? -------------------------------------------------------

    /** Poziom izolacji połączenia w transakcji z domyślnym ustawieniem (Isolation.DEFAULT = „jak baza”). */
    @Transactional
    public int connectionIsolationDefault() {
        return jdbcTemplate.execute((ConnectionCallback<Integer>) Connection::getTransactionIsolation);
    }

    /** Poziom izolacji połączenia przy SERIALIZABLE (szeregowalny) — ale tylko gdy to NOWA transakcja! */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public int connectionIsolationSerializable() {
        return jdbcTemplate.execute((ConnectionCallback<Integer>) Connection::getTransactionIsolation);
    }

    // --- timeout ----------------------------------------------------------------------------------------------

    /**
     * Limit czasu zapytania (query timeout, w sekundach), jaki Spring sam ustawił na instrukcji JDBC w transakcji
     * z {@code timeout = 5}: to czas, który transakcji JESZCZE ZOSTAŁ (zaokrąglony w górę do pełnych sekund).
     */
    @Transactional(timeout = 5)
    public int queryTimeoutWithTransactionTimeout() {
        return jdbcTemplate.execute((StatementCallback<Integer>) Statement::getQueryTimeout);
    }
}
