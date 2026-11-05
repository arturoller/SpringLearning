package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sekcja 2 lekcji Tx03ProxyPitfalls — obejście self-invocation przez wstrzyknięcie SAMEGO SIEBIE (czyli swojego proxy).
 * Działa, ale to obejście, nie wzorzec: klasa zależy od siebie, a czytelnik kodu musi wiedzieć, dlaczego
 * {@code self.metoda()} różni się od {@code metoda()}. Najpierw rozważ osobny bean (PriceUpdater).
 */
@Service
public class SelfInjectingPriceService {

    private final JdbcClient jdbc;
    private final SelfInjectingPriceService self;                   // @Lazy: pośrednik, który sięgnie po proxy przy 1. użyciu
    private final ObjectProvider<SelfInjectingPriceService> selfProvider; // object provider = dostawca obiektu (na żądanie)

    /**
     * {@code @Lazy} (leniwy) — zamiast gotowego beana (którego jeszcze nie ma, bo właśnie go tworzymy) Spring wstrzykuje
     * leniwego pośrednika; bez tego start kończy się błędem cyklicznej zależności (Boot domyślnie ich zabrania).
     */
    public SelfInjectingPriceService(JdbcClient jdbc, @Lazy SelfInjectingPriceService self,
                                     ObjectProvider<SelfInjectingPriceService> selfProvider) {
        this.jdbc = jdbc;
        this.self = self;
        this.selfProvider = selfProvider;
    }

    /** Przez {@code @Lazy self} — wywołanie trafia do proxy, transakcja jest (test). */
    public TxState stateViaLazySelf() {
        return self.stateInTransaction();
    }

    /** Przez {@code ObjectProvider.getObject()} — bean (proxy) pobierany z kontenera w chwili wywołania (test). */
    public TxState stateViaObjectProvider() {
        return selfProvider.getObject().stateInTransaction();
    }

    /** Podwyżka przez self — przy wyjątku rollback wszystkich zmian (test). */
    public void raiseAllViaSelf(int percent) {
        self.raiseAllInTransaction(percent);
    }

    @Transactional
    public TxState stateInTransaction() {
        return TxState.now();
    }

    @Transactional
    public void raiseAllInTransaction(int percent) {
        PriceUpdater.raiseAllWith(jdbc, percent);
    }
}
