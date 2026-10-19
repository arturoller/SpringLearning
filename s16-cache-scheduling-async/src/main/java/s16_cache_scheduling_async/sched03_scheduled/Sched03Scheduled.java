package s16_cache_scheduling_async.sched03_scheduled;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * <pre>
 * TEMAT: Zadania cykliczne — @EnableScheduling i @Scheduled
 *        (scheduling = planowanie, harmonogram; fixed rate = stała częstotliwość; fixed delay = stała przerwa;
 *         initial delay = opóźnienie początkowe; cron = wyrażenie harmonogramu; zone = strefa czasowa)
 *
 * W SKRÓCIE:
 *   Czasem aplikacja ma coś robić sama, o określonych porach: co 30 minut odświeżyć stany magazynowe, co noc
 *   posprzątać pliki, w dni robocze o 8:00 wysłać raport. Dopisujesz @Scheduled do bezparametrowej metody beana,
 *   włączasz @EnableScheduling, a Spring sam wywołuje metodę zgodnie z harmonogramem w osobnym wątku.
 *   Testowanie nie polega na czekaniu: czytamy ustawienia zadań, liczymy terminy cron dla ustalonej chwili i wołamy
 *   logikę zadania z zegarem Clock.fixed.
 *
 * ANALOGIA: budzik i kartka „co robić o której” w sklepie.
 *   fixedRate = „co 30 minut sprawdź półki” (liczysz od początku poprzedniego sprawdzenia);
 *   fixedDelay = „30 minut po skończeniu sprzątania znów posprzątaj”; cron = wpis w kalendarzu „pon–pt o 8:00”.
 *   Jeden pracownik (jeden wątek) = dwa zadania naraz nie zrobi — drugie czeka, aż pierwsze skończy.
 *
 * JAK TO DZIAŁA:
 *   1. @EnableScheduling włącza ScheduledAnnotationBeanPostProcessor: skanuje beany w poszukiwaniu @Scheduled
 *      i rejestruje każde zadanie (FixedRateTask, FixedDelayTask albo CronTask) w harmonogramie.
 *   2. Boot (TaskSchedulingAutoConfiguration) tworzy bean "taskScheduler" — ThreadPoolTaskScheduler z JEDNYM wątkiem
 *      o nazwach scheduling-1 (spring.task.scheduling.pool.size=1, thread-name-prefix=scheduling-). Sprawdzone.
 *   3. W określonej chwili wątek harmonogramu wywołuje metodę. Wszystkie zadania dzielą ten jeden wątek.
 *   4. Zarejestrowane zadania można odczytać: bean ScheduledTaskHolder (to ScheduledAnnotationBeanPostProcessor)
 *      → getScheduledTasks() → ScheduledTask.getTask() → FixedRateTask.getIntervalDuration(), CronTask.getExpression().
 *
 * SŁÓWKA:
 *   scheduled = zaplanowany; fixed rate = stała częstotliwość; fixed delay = stała przerwa; initial delay = opóźnienie
 *   początkowe; cron = wyrażenie czasowe (nazwa od unixowego narzędzia cron); zone = strefa czasowa; thread pool =
 *   pula wątków; task = zadanie; Clock = zegar (obiekt dostarczający aktualny czas); daylight saving time = czas letni.
 *
 * ZOBACZ TEŻ: s16_cache_scheduling_async/Async04Async (praca w tle na innych wątkach),
 *             s16_cache_scheduling_async/Cache02CacheManagement (cykliczne czyszczenie pamięci),
 *             t17_datetime/DateTime04ZonesInstant (strefy i Instant), t21_concurrency/Concurrency09ScheduledForkJoin
 *             (harmonogram bez Springa).
 * </pre>
 */
public final class Sched03Scheduled {

    private Sched03Scheduled() {
    }

    // =================================================================================================
    // 1. TRZY RODZAJE HARMONOGRAMU (StockReportTasks, test 1)
    // =================================================================================================
    //
    //   @Scheduled(fixedRate = 30, initialDelay = 60, timeUnit = TimeUnit.MINUTES)   co 30 min od POCZĄTKU poprzedniego
    //   @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1H")           5 min po ZAKOŃCZENIU poprzedniego
    //   @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "Europe/Warsaw")               8:00 w dni robocze, czas warszawski
    //
    // Metoda musi być BEZPARAMETROWA (zwracana wartość jest ignorowana). initialDelay mówi, ile czekać przed
    // pierwszym uruchomieniem (domyślnie: od razu po starcie). Wersje „...String” przyjmują tekst: liczbę
    // milisekund, zapis ISO-8601 (PT5M = 5 minut) albo ${placeholder} z application.properties — dzięki temu
    // częstotliwość można zmienić bez kompilacji. Sprawdzone: test 1 odczytuje 30 minut i 5 minut z zarejestrowanych zadań.
    //
    // Nowość względem starszych tutoriali: atrybut timeUnit (jednostka dla fixedRate/fixedDelay/initialDelay),
    // atrybut scheduler (nazwa konkretnego beana TaskScheduler) i to, że @Scheduled można powtórzyć na jednej metodzie.
    //
    // fixedRate czy fixedDelay? fixedRate = „co 30 minut”, niezależnie od czasu trwania zadania; fixedDelay = „odstęp 5
    // minut między końcem a początkiem” — bezpieczniejszy, gdy zadanie bywa długie i nie chcesz „nadgonienia”.
    //
    // DOBRA PRAKTYKA: wartości harmonogramu trzymaj w konfiguracji (fixedRateString = "${sklep.odswiezanie:PT30M}").
    //   Dlaczego? Zmiana częstotliwości na produkcji nie wymaga budowania nowej wersji, a test może ustawić długi okres.

    // =================================================================================================
    // 2. CRON W SPRINGU — 6 PÓL (test 2, 3, 4, 5)
    // =================================================================================================
    //
    //   ┌ sekundy  ┌ minuty  ┌ godziny  ┌ dzień miesiąca  ┌ miesiąc  ┌ dzień tygodnia
    //   0          0         8          *                 *         MON-FRI
    //
    // Cron w Springie ma SZEŚĆ pól (z sekundami). Cron Linuksa (crontab) ma PIĘĆ — bez sekund.
    //
    // PUŁAPKA: przepisanie wyrażenia z crontaba Linuksa ("0 8 * * MON-FRI"). Dlaczego? Spring odrzuca je przy starcie
    //   (IllegalArgumentException: wyrażenie musi mieć 6 pól — test 3), a gdyby pól było „akurat” 6, znaczyłyby zupełnie
    //   co innego (pierwsze to sekundy).
    //
    // PUŁAPKA: gwiazdka w polu sekund, np. "* * 8 * * *". Dlaczego? To NIE „raz o 8:00”, tylko „co sekundę przez całą
    //   godzinę 8:00–8:59” (test 4: po 8:00:00 następny termin to 8:00:01). Na początku wpisz 0 w sekundach i minutach.
    //
    // Przydatne: "0 */15 * * * *" co 15 minut, "0 0 2 * * *" codziennie o 2:00, "0 0 8 * * MON-FRI" dni robocze,
    // makra: "@daily", "@hourly". Wyrażenie sprawdza CronExpression.isValidExpression(...), a termin liczy
    // CronExpression.parse(...).next(chwila) — dla USTALONEJ chwili, więc test nie czeka na prawdziwy czas.
    //
    // PUŁAPKA: brak strefy. Dlaczego? Bez zone= cron liczy się w strefie serwera. Serwer w chmurze zwykle ma UTC, więc
    //   „8:00” staje się 9:00 lub 10:00 w Warszawie i przesuwa się dwa razy w roku (czas letni/zimowy; test 5:
    //   8:00 w Warszawie to 06:00 UTC latem i 07:00 UTC zimą). Podawaj zone = "Europe/Warsaw" jawnie.

    // =================================================================================================
    // 3. POJEDYNCZY WĄTEK HARMONOGRAMU (test 7, 11)
    // =================================================================================================
    //
    // Domyślnie jest JEDEN wątek (spring.task.scheduling.pool.size=1; sprawdzone w teście 7: getCorePoolSize() = 1,
    // wątek nazywa się scheduling-1). Wszystkie zadania cykliczne aplikacji stoją w jednej kolejce.
    //
    // PUŁAPKA: długie zadanie blokuje pozostałe. Dlaczego? Jeśli „raport poranny” trwa 10 minut, to „odświeżanie
    //   stanów” zaplanowane w tym czasie czeka, aż jedyny wątek się zwolni — spóźnia się i nikt tego nie widzi.
    //   Zwiększ pulę (spring.task.scheduling.pool.size=4 — test 11) albo wynieś ciężką pracę do @Async (Async04Async).
    //
    // Wyjątek w zadaniu powtarzalnym jest tylko LOGOWANY (komunikat „Unexpected error occurred in scheduled task”),
    // a harmonogram uruchamia je dalej (test 8: trzy uruchomienia mimo wyjątku w pierwszym).
    // PUŁAPKA: zadanie, które zawsze rzuca wyjątek, „działa” — a w logach tylko powtarzający się błąd. Dlaczego?
    //   Nikt nie dostaje powiadomienia. Łap wyjątki w zadaniu i raportuj (metryki, alert), jeśli ma znaczenie.
    //
    // Błąd konfiguracji (metoda z parametrem) kończy się wyjątkiem już przy STARCIE kontekstu (test 9):
    //   „Could not create recurring task for @Scheduled method 'withParameter': Only no-arg methods may be annotated
    //   with @Scheduled” — dobrze, bo błąd nie czeka do godziny 8:00.

    // =================================================================================================
    // 4. ZEGAR I TESTOWANIE (test 1, 6)
    // =================================================================================================
    //
    // DOBRA PRAKTYKA: logikę zadania wywołuj przez wstrzyknięty Clock (Instant.now(clock)), nie przez
    //   LocalDate.now(). Dlaczego? Test podaje Clock.fixed(...) i dostaje powtarzalny wynik (test 6), bez czekania
    //   do ósmej rano i bez zależności od dnia, w którym uruchomiono testy.
    //
    // DOBRA PRAKTYKA: odczytuj konfigurację z ScheduledTaskHolder zamiast czekać na uruchomienie (test 1).
    //   Dlaczego? Test „poczekaj aż się uruchomi” jest wolny i niestabilny (zależy od obciążenia komputera).
    //
    // PUŁAPKA: aplikacja w kilku instancjach (np. 3 kontenery). Dlaczego? Każda instancja ma WŁASNY harmonogram
    //   w swojej JVM, więc raport o 8:00 wyśle się trzy razy. Rozwiązania poza zakresem tego modułu: blokada
    //   rozproszona (np. biblioteka ShedLock) albo zewnętrzny harmonogram (CronJob w Kubernetes).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @EnableScheduling + @Scheduled na bezparametrowej metodzie beana (zwracana wartość jest ignorowana).
     *   • fixedRate = od początku poprzedniego, fixedDelay = od końca poprzedniego, initialDelay = opóźnienie startu;
     *     wersje ...String przyjmują ISO-8601 (PT5M) i ${placeholdery}.
     *   • cron Springa: 6 pól (sekundy minuty godziny dzień miesiąc dzień-tygodnia); Linuks ma 5. Zawsze podaj zone.
     *   • Pula harmonogramu: domyślnie 1 wątek "scheduling-N"; zmiana: spring.task.scheduling.pool.size.
     *   • Wyjątek w zadaniu powtarzalnym jest logowany, kolejne uruchomienia trwają dalej.
     *   • Test: ScheduledTaskHolder (konfiguracja), CronExpression.next(ustalona chwila), Clock.fixed w logice.
     *   • Wiele instancji aplikacji = wiele harmonogramów (blokada rozproszona poza zakresem lekcji).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się fixedRate od fixedDelay?
     *   2. Co się stanie? Wpiszesz cron "0 8 * * MON-FRI" (zapis z Linuksa) w @Scheduled — kiedy i jak zobaczysz błąd?
     *   3. Co się stanie? Cron "* * 8 * * *" — jak często uruchomi się zadanie?
     *   4. ZNAJDŹ BŁĄD: zadanie z @Scheduled(cron = "0 0 8 * * *") działa na serwerze w UTC, a biznes narzeka, że raport
     *      przychodzi o 10:00 latem. Dlaczego i co dopisać?
     *   5. Co się stanie? Metoda @Scheduled(fixedRate = 1000) ma parametr String sku.
     *   6. ZNAJDŹ BŁĄD: dwa zadania cykliczne, jedno trwa 10 minut; drugie „co minutę” uruchamia się co 10 minut.
     *      Dlaczego i jak to naprawić?
     *   7. Co się stanie z zadaniem powtarzalnym, które w pierwszym uruchomieniu rzuciło wyjątek?
     *   8. Jak przetestować zadanie „codziennie o 8:00” bez czekania na ósmą?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Sched03ScheduledExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wyrażenie cron Springa (6 pól) na „co poniedziałek o 6:30:00”.
     * Podpowiedź: sekcja 2; dzień tygodnia można zapisać jako MON.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć listę {@code n} kolejnych terminów wyrażenia {@code cron}, licząc od chwili
     * {@code from} (bez niej samej — pierwszy termin jest STRICTE późniejszy).
     * Podpowiedź: {@code CronExpression.parse(cron).next(chwila)} zwraca kolejny termin lub null; wołaj w pętli.
     */
    static List<ZonedDateTime> exercise2(ZonedDateTime from, String cron, int n) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć najbliższy termin wyrażenia {@code cron} w strefie {@code zone} jako
     * {@link Instant}, licząc od chwili {@code now}. Wyrażenie jest interpretowane w strefie {@code zone}.
     * Przykład: now = 2026-07-14T20:00:00Z, cron "0 0 8 * * *", zone Europe/Warsaw → 2026-07-15T06:00:00Z.
     * Podpowiedź: {@code now.atZone(zone)}, potem next(...), potem {@code toInstant()}; zobacz test 5 lekcji.
     */
    static Instant exercise3(Instant now, String cron, ZoneId zone) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. fixedRate: odstęp liczony od POCZĄTKU poprzedniego uruchomienia (stała częstotliwość); fixedDelay: odstęp
     *      liczony od ZAKOŃCZENIA poprzedniego (stała przerwa, uruchomienia nie nakładają się).
     *   2. Błąd zobaczysz od razu przy starcie kontekstu (nie o 8:00): IllegalArgumentException, że wyrażenie musi mieć
     *      6 pól (komunikat zawiera „6 fields”). Sprawdzone testem 3 na CronExpression.parse; to samo parsowanie
     *      wykonuje @Scheduled przy rejestracji zadania.
     *   3. Co SEKUNDĘ przez całą godzinę 8:00–8:59 (po 8:00:00 następny termin to 8:00:01). Sprawdzone testem 4.
     *   4. Cron liczy się w strefie serwera (UTC), a w Warszawie latem jest UTC+2, więc 8:00 UTC to 10:00 w Warszawie.
     *      Dopisz zone = "Europe/Warsaw". Sprawdzone testem 5 (8:00 Warszawa = 06:00Z latem, 07:00Z zimą).
     *   5. Kontekst nie wystartuje: BeanCreationException z komunikatem „Could not create recurring task for @Scheduled
     *      method 'withParameter': Only no-arg methods may be annotated with @Scheduled”. Sprawdzone testem 9.
     *   6. Pula harmonogramu ma domyślnie jeden wątek (test 7), więc krótkie zadanie czeka za długim. Naprawa:
     *      spring.task.scheduling.pool.size większe niż 1 (test 11) albo przeniesienie ciężkiej pracy do @Async.
     *   7. Wyjątek jest zalogowany, a zadanie uruchamia się dalej według harmonogramu. Sprawdzone testem 8
     *      (trzy uruchomienia, pierwsze z wyjątkiem).
     *   8. Wywołaj logikę z Clock.fixed (test 6), sprawdź konfigurację z ScheduledTaskHolder (test 1), a termin policz
     *      CronExpression.next(ustalona chwila) (testy 2 i 5) — bez czekania na prawdziwy czas.
     */
    // </editor-fold>
}
