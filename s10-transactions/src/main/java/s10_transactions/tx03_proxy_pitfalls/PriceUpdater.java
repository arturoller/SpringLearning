package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Podwyżka cen (price updater = aktualizator cen) — OSOBNY bean z metodą transakcyjną. Sekcja 2 lekcji
 * Tx03ProxyPitfalls, DOBRA PRAKTYKA: kod, który ma działać w transakcji, wydziel do innego beana — wtedy wywołanie
 * zawsze idzie przez proxy.
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class PriceUpdater {

    /** Maksymalna cena po podwyżce. */
    public static final BigDecimal LIMIT = new BigDecimal("900.00");

    private final JdbcClient jdbc; // JdbcClient = klient JDBC (s07_data_jdbc/Jdbc01JdbcClient)

    public PriceUpdater(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Podnosi ceny po kolei (wg id). Za drogi produkt (Monitor) → wyjątek; z transakcją — rollback wszystkich zmian. */
    @Transactional
    public void raiseAll(int percent) {
        raiseAllWith(jdbc, percent);
    }

    /** Wspólny kod podwyżki — używany też przez PriceService (wersje z pułapkami). */
    static void raiseAllWith(JdbcClient jdbc, int percent) {
        List<Product> products = jdbc.sql("SELECT id, name, price FROM product ORDER BY id").query(Product.class).list();
        BigDecimal factor = BigDecimal.valueOf(100 + percent).movePointLeft(2);
        for (Product product : products) {
            BigDecimal newPrice = product.price().multiply(factor).setScale(2, RoundingMode.HALF_UP);
            if (newPrice.compareTo(LIMIT) > 0) {
                throw new PriceLimitExceededException(product.name(), newPrice);
            }
            jdbc.sql("UPDATE product SET price = ? WHERE id = ?").params(newPrice, product.id()).update();
        }
    }

    /** Wiersz tabeli product. */
    public record Product(Long id, String name, BigDecimal price) {
    }
}
