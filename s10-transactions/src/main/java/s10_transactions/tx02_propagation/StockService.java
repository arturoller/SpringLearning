package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Magazyn (stock = zapas, stan magazynu) — wewnętrzny serwis lekcji Tx02Propagation. Woła go OrderService, więc
 * jego metody transakcyjne wykonują się ZAZWYCZAJ wewnątrz transakcji zamówienia — i właśnie o to chodzi
 * w propagacji (propagation = rozprzestrzenianie: co zrobić, gdy transakcja już trwa).
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class StockService {

    private final JdbcClient jdbc; // JdbcClient = klient JDBC (s07_data_jdbc/Jdbc01JdbcClient)

    public StockService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Sekcje 1–2: REQUIRED (domyślna propagacja — nie trzeba jej pisać): dołącz do trwającej transakcji, a gdy jej
     * nie ma — otwórz nową. Brak towaru → OutOfStockException, który wychodzi przez proxy TEJ metody.
     */
    @Transactional // = @Transactional(propagation = Propagation.REQUIRED)
    public void reserve(String product, int quantity) {
        if (!decrease(product, quantity)) {
            throw new OutOfStockException(product, quantity);
        }
    }

    /** Sekcja 2, PO: zamiast wyjątku — wynik. Wywołujący sam decyduje, co dalej, a transakcja nie jest „zatruta”. */
    @Transactional
    public boolean tryReserve(String product, int quantity) {
        return decrease(product, quantity);
    }

    /**
     * Sekcja 3, PUŁAPKA: korekta stanu w NOWEJ transakcji (REQUIRES_NEW). Wywołana z transakcji, która już zmieniła
     * ten sam wiersz, czeka na blokadę trzymaną przez… zawieszoną transakcję zewnętrzną — zakleszczenie (test).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void correctInNewTransaction(String product, int delta) {
        jdbc.sql("UPDATE stock SET quantity = quantity + :delta WHERE product = :product")
                .param("delta", delta).param("product", product).update();
    }

    /** Sekcja 4: NESTED z domyślnym menedżerem (JpaTransactionManager) — NIE działa (test). */
    @Transactional(propagation = Propagation.NESTED)
    public void reserveNestedWithJpaManager(String product, int quantity) {
        reserve0(product, quantity);
    }

    /**
     * Sekcja 4: NESTED z menedżerem JDBC — zagnieżdżona transakcja = SAVEPOINT (punkt zapisu) w tej samej transakcji.
     * Wyjątek wycofuje tylko zmiany od savepointu; zewnętrzna transakcja może iść dalej (test).
     */
    @Transactional(transactionManager = "jdbcTransactionManager", propagation = Propagation.NESTED)
    public void reserveGiftNested(String product) {
        reserve0(product, 1);
    }

    private void reserve0(String product, int quantity) {
        if (!decrease(product, quantity)) {
            throw new OutOfStockException(product, quantity);
        }
    }

    /** UPDATE tylko wtedy, gdy towaru wystarczy; true = zarezerwowano. */
    private boolean decrease(String product, int quantity) {
        return jdbc.sql("UPDATE stock SET quantity = quantity - :quantity WHERE product = :product AND quantity >= :quantity")
                .param("quantity", quantity).param("product", product).update() == 1;
    }
}
