package s01_core_ioc.ioc07_lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * WarehouseConnection = połączenie z magazynem. Bean, który przechodzi po kolei wszystkie etapy cyklu życia
 * i zapisuje je w EventLog:
 * <ol>
 *   <li>"konstruktor" — obiekt jest tworzony; ma tylko to, co przyszło przez konstruktor (EventLog),</li>
 *   <li>"setter" — Spring wstrzykuje resztę zależności przez metodę set... ({@code @Autowired}),</li>
 *   <li>"@PostConstruct" — (post construct = po skonstruowaniu) wszystko jest już wstrzyknięte, można zacząć pracę,</li>
 *   <li>"@PreDestroy" — (pre destroy = przed zniszczeniem) kontekst się zamyka, czas zwolnić zasoby.</li>
 * </ol>
 */
@Component
public class WarehouseConnection {

    private final EventLog log;
    private ProductCatalog catalog;

    private boolean catalogReadyInPostConstruct;
    private boolean connected;

    public WarehouseConnection(EventLog log) {
        this.log = log;
        // katalog jest wstrzykiwany setterem, czyli PÓŹNIEJ niż działa konstruktor: tutaj jeszcze go nie ma
        // (dowód: w dzienniku wpis "konstruktor" stoi przed wpisem "setter")
        log.add("konstruktor");
    }

    @Autowired
    public void setCatalog(ProductCatalog catalog) {
        this.catalog = catalog;
        log.add("setter");
    }

    @PostConstruct
    void connect() {
        // tu wszystkie zależności są już na miejscu
        this.catalogReadyInPostConstruct = (catalog != null);
        this.connected = true;
        log.add("@PostConstruct");
    }

    @PreDestroy
    void disconnect() {
        this.connected = false;
        log.add("@PreDestroy");
    }

    public boolean catalogReadyInPostConstruct() {
        return catalogReadyInPostConstruct;
    }

    public boolean isConnected() {
        return connected;
    }
}
