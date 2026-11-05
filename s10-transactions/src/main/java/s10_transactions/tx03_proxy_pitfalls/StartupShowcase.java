package s10_transactions.tx03_proxy_pitfalls;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sekcja 4 lekcji Tx03ProxyPitfalls: kod startowy. {@code @PostConstruct} (po utworzeniu) działa na SUROWYM obiekcie,
 * zanim powstanie proxy — {@code @Transactional} na nim nie działa. Zdarzenie {@code ApplicationReadyEvent}
 * (aplikacja gotowa) przychodzi, gdy wszystkie beany i proxy już istnieją, a Spring woła słuchacza na beanie
 * z kontenera, czyli na proxy — transakcja jest (testy).
 */
@Service
public class StartupShowcase {

    private TxState seenInPostConstruct;
    private TxState seenOnApplicationReady;

    @PostConstruct
    @Transactional // ignorowana — proxy jeszcze nie ma, a wywołanie idzie bezpośrednio na obiekt
    public void warmUpCache() {
        seenInPostConstruct = TxState.now();
    }

    @EventListener(ApplicationReadyEvent.class) // event listener = słuchacz zdarzeń
    @Transactional
    public void onApplicationReady() {
        seenOnApplicationReady = TxState.now();
    }

    public TxState seenInPostConstruct() {
        return seenInPostConstruct;
    }

    public TxState seenOnApplicationReady() {
        return seenOnApplicationReady;
    }
}
