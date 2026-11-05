package s10_transactions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dziennik audytu (audit log = dziennik zdarzeń) — sekcja 3 lekcji Tx02Propagation. Wpis ma zostać w bazie NIEZALEŻNIE
 * od tego, czy zamówienie się uda: dlatego REQUIRES_NEW (requires new = wymaga nowej): zawsze WŁASNA, nowa
 * transakcja; trwająca transakcja wywołującego jest na ten czas ZAWIESZANA (suspend).
 */
@Service
public class AuditLog {

    private final JdbcClient jdbc;

    public AuditLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Zapis w osobnej transakcji — commit od razu po powrocie z metody, zanim skończy się transakcja zamówienia. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String message) {
        jdbc.sql("INSERT INTO audit_log (message) VALUES (?)").param(message).update();
    }

    /**
     * Co widać z wnętrza NOWEJ transakcji: numer sesji H2 (= połączenia), liczba zamówień widocznych w bazie i stan
     * transakcji. Wywołana z transakcji zamówienia pokazuje INNE połączenie i NIE widzi niezatwierdzonego zamówienia (test).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NewTransactionView inspectFromNewTransaction() {
        int sessionId = jdbc.sql("SELECT SESSION_ID()").query(Integer.class).single();
        long orders = jdbc.sql("SELECT COUNT(*) FROM shop_order").query(Long.class).single();
        return new NewTransactionView(sessionId, orders, TxState.now());
    }

    /** Wynik {@link #inspectFromNewTransaction()}. */
    public record NewTransactionView(int sessionId, long visibleOrders, TxState state) {
    }
}
