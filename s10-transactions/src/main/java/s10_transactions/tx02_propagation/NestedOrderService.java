package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sekcja 4 lekcji Tx02Propagation — NESTED (zagnieżdżona): zamówienie z OPCJONALNYM prezentem. Brak prezentu na
 * magazynie nie może zepsuć zamówienia: rezerwacja prezentu działa od savepointu i w razie błędu wycofuje się sama.
 */
@Service
public class NestedOrderService {

    private final JdbcClient jdbc;
    private final StockService stock;

    public NestedOrderService(JdbcClient jdbc, StockService stock) {
        this.jdbc = jdbc;
        this.stock = stock;
    }

    /**
     * Transakcja menedżera JDBC (savepointy działają). Zwraca true, gdy prezent zarezerwowano. Brak prezentu:
     * rollback do savepointu, zamówienie i rezerwacja produktu zostają (test).
     */
    @Transactional(transactionManager = "jdbcTransactionManager")
    public boolean placeOrderWithOptionalGift(String orderNumber, String product, String gift) {
        jdbc.sql("INSERT INTO shop_order (order_number, status) VALUES (?, 'RESERVED')").param(orderNumber).update();
        // Produkt rezerwujemy tu, JDBC-em, a nie przez stock.reserve(): tamta metoda używa DOMYŚLNEGO menedżera (JPA),
        // a dwóch menedżerów na jednym DataSource w jednej transakcji mieszać nie wolno (placeOrderMixingManagers).
        jdbc.sql("UPDATE stock SET quantity = quantity - 1 WHERE product = ?").param(product).update();
        try {
            stock.reserveGiftNested(gift); // NESTED: SAVEPOINT → UPDATE → (wyjątek) ROLLBACK TO SAVEPOINT
            return true;
        } catch (OutOfStockException e) {
            return false; // tu łapanie jest bezpieczne: NESTED wycofał tylko swoją część, a nie oznaczył całości
        }
    }

    /**
     * Sekcja 4, PUŁAPKA: transakcja menedżera JDBC woła metodę z {@code @Transactional} DOMYŚLNEGO menedżera (JPA).
     * JpaTransactionManager nie widzi „swojej” transakcji, próbuje otworzyć nową i trafia na połączenie już przypięte
     * do wątku przez menedżer JDBC → {@code CannotCreateTransactionException} (test).
     */
    @Transactional(transactionManager = "jdbcTransactionManager")
    public void placeOrderMixingManagers(String orderNumber, String product) {
        jdbc.sql("INSERT INTO shop_order (order_number, status) VALUES (?, 'RESERVED')").param(orderNumber).update();
        stock.reserve(product, 1);
    }

    /** Ta sama próba z domyślnym JpaTransactionManager: NESTED rzuca NestedTransactionNotSupportedException (test). */
    @Transactional
    public void placeOrderWithGiftUsingJpaManager(String orderNumber, String gift) {
        jdbc.sql("INSERT INTO shop_order (order_number, status) VALUES (?, 'RESERVED')").param(orderNumber).update();
        stock.reserveNestedWithJpaManager(gift, 1);
    }
}
