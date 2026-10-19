package s16_cache_scheduling_async.sched03_scheduled;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Klasa celowo ZŁA — używa jej tylko test sekcji o błędach (rejestrowana ręcznie, NIE jest komponentem, więc skanowanie
 * pakietu jej nie widzi). Metoda @Scheduled z parametrem jest niedozwolona.
 */
public class BadScheduledBean {

    @Scheduled(fixedRate = 1000)
    public void withParameter(String sku) {
    }
}
