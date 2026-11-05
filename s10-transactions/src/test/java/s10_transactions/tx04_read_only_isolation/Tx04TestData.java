package s10_transactions.tx04_read_only_isolation;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;

/**
 * Dane testowe lekcji Tx04ReadOnlyIsolation. Testy działają BEZ transakcji testu, więc przed każdym przywracamy stan
 * z migracji V2. Odczyty JdbcClientem spoza transakcji widzą tylko dane ZATWIERDZONE.
 */
public final class Tx04TestData {

    private Tx04TestData() {
    }

    public static void reset(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM wallet").update();
        jdbc.sql("DELETE FROM versioned_wallet").update();
        jdbc.sql("INSERT INTO wallet (id, owner, balance) VALUES (1, 'Anna Nowak', 100.00), (2, 'Jan Kowalski', 50.00)")
                .update();
        jdbc.sql("INSERT INTO versioned_wallet (id, owner, balance, version) "
                + "VALUES (1, 'Anna Nowak', 100.00, 0), (2, 'Jan Kowalski', 50.00, 0)").update();
    }

    public static BigDecimal balance(JdbcClient jdbc, long walletId) {
        return jdbc.sql("SELECT balance FROM wallet WHERE id = ?").param(walletId).query(BigDecimal.class).single();
    }

    public static String owner(JdbcClient jdbc, long walletId) {
        return jdbc.sql("SELECT owner FROM wallet WHERE id = ?").param(walletId).query(String.class).single();
    }

    public static BigDecimal versionedBalance(JdbcClient jdbc, long walletId) {
        return jdbc.sql("SELECT balance FROM versioned_wallet WHERE id = ?").param(walletId).query(BigDecimal.class)
                .single();
    }

    public static long version(JdbcClient jdbc, long walletId) {
        return jdbc.sql("SELECT version FROM versioned_wallet WHERE id = ?").param(walletId).query(Long.class).single();
    }
}
