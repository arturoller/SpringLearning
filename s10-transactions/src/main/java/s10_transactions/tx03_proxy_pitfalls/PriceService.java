package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis cen lekcji Tx03ProxyPitfalls — sekcje 1, 2 i 4: wywołanie metody transakcyjnej z TEJ SAMEJ klasy
 * (self-invocation = wywołanie samego siebie) i obiekt utworzony przez {@code new}.
 */
@Service
public class PriceService {

    private final JdbcClient jdbc;
    private final PriceUpdater updater;

    public PriceService(JdbcClient jdbc, PriceUpdater updater) {
        this.jdbc = jdbc;
        this.updater = updater;
    }

    /**
     * Sekcja 2, PUŁAPKA: metoda bez transakcji woła {@code this.raiseAllInTransaction(...)}. {@code this} to obiekt,
     * NIE proxy — adnotacja jest ignorowana, każdy UPDATE zatwierdza się sam. Wyjątek przy Monitorze zostawia
     * podwyżkę klawiatury i myszy w bazie (test).
     */
    public void raiseAllSelfInvoked(int percent) {
        raiseAllInTransaction(percent); // = this.raiseAllInTransaction(percent)
    }

    /** Wywołana z ZEWNĄTRZ (przez proxy) — działa w transakcji: przy wyjątku nic się nie zmienia (test). */
    @Transactional
    public void raiseAllInTransaction(int percent) {
        PriceUpdater.raiseAllWith(jdbc, percent);
    }

    /** Sekcja 2, PO: delegacja do innego beana — wywołanie przez jego proxy, transakcja działa (test). */
    public void raiseAllViaSeparateBean(int percent) {
        updater.raiseAll(percent);
    }

    /** Sekcja 2: stan transakcji widziany przez metodę transakcyjną wywołaną przez {@code this} → brak transakcji (test). */
    public TxState stateSelfInvoked() {
        return stateInTransaction();
    }

    @Transactional
    public TxState stateInTransaction() {
        return TxState.now();
    }
}
