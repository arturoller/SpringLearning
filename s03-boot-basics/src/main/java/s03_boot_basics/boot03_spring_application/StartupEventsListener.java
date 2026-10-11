package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * StartupEventsListener = nasłuchiwacz zdarzeń startu. {@code @EventListener} (nasłuchiwacz zdarzeń) wywołuje metodę,
 * gdy Spring opublikuje zdarzenie danego typu. Zapisujemy trzy zdarzenia z końca startu, żeby zobaczyć, gdzie
 * między nimi wykonują się runnery.
 *
 * <p>Wcześniejszych zdarzeń (ApplicationStartingEvent, ApplicationEnvironmentPreparedEvent...) bean NIE zobaczy —
 * publikowane są, zanim kontekst i beany w ogóle istnieją. Do nich służy SpringApplication.addListeners(...).</p>
 */
@Component
public class StartupEventsListener {

    private final RunLog runLog;

    public StartupEventsListener(RunLog runLog) {
        this.runLog = runLog;
    }

    @EventListener
    public void onRefreshed(ContextRefreshedEvent event) {    // refreshed = odświeżony: wszystkie beany utworzone
        runLog.add("ContextRefreshedEvent");
    }

    @EventListener
    public void onStarted(ApplicationStartedEvent event) {    // started = wystartowana: kontekst gotowy, PRZED runnerami
        runLog.add("ApplicationStartedEvent");
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {        // ready = gotowa: PO runnerach, aplikacja przyjmuje pracę
        runLog.add("ApplicationReadyEvent");
    }
}
