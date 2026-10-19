package s16_cache_scheduling_async.sched03_scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Zadania cykliczne sklepu. Każde zapisuje w liście chwilę wykonania odczytaną z wstrzykniętego {@link Clock} —
 * dzięki temu test woła metodę wprost z zegarem „zamrożonym” i nie czeka na prawdziwy czas.
 */
@Component
public class StockReportTasks {

    private final Clock clock;
    private final List<String> log = new CopyOnWriteArrayList<>();

    public StockReportTasks(Clock clock) {
        this.clock = clock;
    }

    public List<String> log() {
        return List.copyOf(log);
    }

    /**
     * fixedRate = stała częstotliwość: kolejne uruchomienie co 30 minut liczone od POCZĄTKU poprzedniego.
     * initialDelay = ile czekać przed pierwszym uruchomieniem (tu godzinę, żeby test nie zobaczył żadnego wykonania).
     */
    @Scheduled(fixedRate = 30, initialDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void refreshStock() {
        log.add("odświeżono stany magazynowe " + Instant.now(clock));
    }

    /**
     * fixedDelay = stała przerwa: kolejne uruchomienie 5 minut po ZAKOŃCZENIU poprzedniego (zadania nie nakładają się).
     * Wersja z tekstem ISO-8601 (PT5M = 5 minut) zamiast liczby i jednostki.
     */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1H")
    public void cleanTemporaryFiles() {
        log.add("posprzątano pliki tymczasowe " + Instant.now(clock));
    }

    /**
     * cron = harmonogram jak w kalendarzu. Spring: 6 pól „sekundy minuty godziny dzień-miesiąca miesiąc dzień-tygodnia”.
     * Tu: 8:00:00 od poniedziałku do piątku w strefie Europe/Warsaw (bez {@code zone} byłaby strefa serwera).
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "Europe/Warsaw")
    public void morningReport() {
        log.add("raport poranny " + Instant.now(clock));
    }
}
