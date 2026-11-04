package s10_transactions.tx01_transactional;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;

/**
 * Dane testowe lekcji Tx01Transactional. Testy NIE działają w transakcji testu (wtedy nie widać commitu i rollbacku),
 * więc zmiany naprawdę trafiają do bazy — przed każdym testem przywracamy stan z migracji V2 (reset).
 * JdbcClient czyta prosto z bazy, czyli widzi tylko to, co zostało ZATWIERDZONE.
 */
public final class Tx01TestData {

    private Tx01TestData() {
    }

    public static void reset(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM wallet").update();
        jdbc.sql("INSERT INTO wallet (id, owner, balance) VALUES (1, 'Anna Nowak', 500.00), (2, 'Jan Kowalski', 100.00)")
                .update();
    }

    public static BigDecimal balance(JdbcClient jdbc, long walletId) {
        return jdbc.sql("SELECT balance FROM wallet WHERE id = ?").param(walletId).query(BigDecimal.class).single();
    }

    public static String owner(JdbcClient jdbc, long walletId) {
        return jdbc.sql("SELECT owner FROM wallet WHERE id = ?").param(walletId).query(String.class).single();
    }
}
