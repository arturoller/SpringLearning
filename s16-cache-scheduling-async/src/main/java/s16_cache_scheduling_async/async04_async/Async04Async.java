package s16_cache_scheduling_async.async04_async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * <pre>
 * TEMAT: Praca w tle — @EnableAsync i @Async
 *        (async = asynchronicznie, „nie czekając”; executor = wykonawca, pula wątków; future = „obietnica wyniku”;
 *         thread = wątek; callback = wywołanie zwrotne)
 *
 * W SKRÓCIE:
 *   Wolna operacja (wysłanie e-maila, generowanie raportu) nie musi blokować wątku, który obsługuje żądanie. Dopisujesz
 *   @Async do metody publicznej beana, włączasz @EnableAsync, a Spring oddaje wywołanie do puli wątków i wraca od razu.
 *   Wynik odbierasz przez CompletableFuture (albo wcale, gdy metoda jest void). Cena: wyjątki, kontekst wątku
 *   (MDC, transakcja, użytkownik zalogowany) i kolejność zdarzeń działają inaczej niż w zwykłym wywołaniu.
 *
 * ANALOGIA: kelner i kuchnia.
 *   Kelner (wątek żądania) przyjmuje zamówienie, wsuwa kartkę przez okienko do kuchni (pula wątków) i od razu
 *   obsługuje następnego klienta; kuchnia gotuje swoim tempem. Kartka to CompletableFuture: kelner może później
 *   sprawdzić „czy już gotowe?”. Gdy kucharz się pomyli (wyjątek), a kelner nie czeka na wynik (metoda void) —
 *   ktoś musi odebrać zgłoszenie błędu (AsyncUncaughtExceptionHandler). Kucharz nie widzi też karteczki z numerem
 *   stolika, którą kelner trzyma w kieszeni (MDC to ThreadLocal wątku kelnera).
 *
 * JAK TO DZIAŁA:
 *   1. @EnableAsync dodaje przetwarzanie @Async: bean z taką metodą jest owijany w PROXY (jak przy @Cacheable).
 *   2. Wywołanie przez proxy pakuje metodę w zadanie i wysyła do wykonawcy (Executor). Boot (TaskExecutionAutoConfiguration)
 *      tworzy bean "applicationTaskExecutor": ThreadPoolTaskExecutor z rdzeniem 8 wątków, NIEOGRANICZONĄ kolejką i
 *      prefiksem nazw "task-" (sprawdzone). Ustawienia: spring.task.execution.pool.*, spring.task.execution.thread-name-prefix.
 *   3. Typ zwracany: void (wynik wyrzucony), Future albo CompletableFuture (wynik odbierasz później).
 *   4. Wyjątek: w CompletableFuture trafia do future (get() rzuca ExecutionException z przyczyną); w metodzie void
 *      — do AsyncUncaughtExceptionHandler (domyślnie tylko log), który podajesz przez AsyncConfigurer.
 *
 * SŁÓWKA:
 *   async = asynchronicznie; executor = wykonawca; thread pool = pula wątków; core size = rozmiar rdzenia puli;
 *   queue capacity = pojemność kolejki; CompletableFuture = obietnica wyniku z łączeniem etapów; MDC = Mapped Diagnostic
 *   Context, mapa diagnostyczna logowania; ThreadLocal = zmienna lokalna wątku; decorator = dekorator (opakowanie);
 *   fire and forget = „wyślij i zapomnij”; proxy = pośrednik.
 *
 * ZOBACZ TEŻ: s16_cache_scheduling_async/Async05VirtualThreads (wątki wirtualne zamiast puli),
 *             s16_cache_scheduling_async/Cache01Cacheable (ta sama pułapka proxy),
 *             s10_transactions/Tx03ProxyPitfalls (transakcja a inny wątek),
 *             s17_events/Evt03AsyncAndOutbox (zdarzenia asynchroniczne),
 *             t21_concurrency/Concurrency04Executors, t21_concurrency/Concurrency05CompletableFuture.
 * </pre>
 */
public final class Async04Async {

    private Async04Async() {
    }

    // =================================================================================================
    // 1. @Async + CompletableFuture (ReportService.buildReport, test 1–2)
    // =================================================================================================
    //
    //   @Async
    //   public CompletableFuture<String> buildReport(String name) { ... }   // wykona się w wątku "task-N"
    //
    //   CompletableFuture<String> f = reports.buildReport("miesięczny");    // wraca od razu
    //   String worker = f.get(10, TimeUnit.SECONDS);                        // czekamy na wynik (limit = bezpiecznik)
    //
    // Metoda ma być PUBLICZNA i wołana PRZEZ PROXY, a zwracać void, Future albo CompletableFuture.
    // W kontrolerze Spring MVC możesz zwrócić CompletableFuture wprost — wątek żądania zostaje zwolniony, a odpowiedź
    // dokończy się, gdy future się zakończy (zobacz ReportController i http/async04_async.http).
    //
    // Domyślny wykonawca: 8 wątków rdzenia i kolejka o pojemności Integer.MAX_VALUE (test 2). Zadania ponad 8
    // równoległych czekają w kolejce.
    //
    // PUŁAPKA: oczekiwanie, że pula „sama urośnie” pod obciążeniem (spring.task.execution.pool.max-size). Dlaczego?
    //   Przy nieograniczonej kolejce pula nie dodaje wątków ponad rdzeń — max-size jest ignorowany (opis
    //   właściwości w metadanych Boota). Chcesz więcej równoległości: zwiększ core-size, albo ogranicz kolejkę
    //   (queue-capacity), wtedy max-size zadziała, a przy przepełnieniu zadanie zostanie odrzucone.
    //
    // PUŁAPKA: własny bean wykonawcy w aplikacji. Dlaczego? Autokonfiguracja Boota tworzy applicationTaskExecutor tylko,
    //   gdy NIE ma innego beana Executor (test 12: po dodaniu własnego bean applicationTaskExecutor znika, a @Async
    //   jedzie na "raport-1"). Skutek: ustawienia spring.task.execution.* przestają działać, a Ty odpowiadasz za
    //   rozmiar puli i nazwy wątków.

    // =================================================================================================
    // 2. WYJĄTKI (test 3–4)
    // =================================================================================================
    //
    //   CompletableFuture: wyjątek trafia do future. f.get() rzuca ExecutionException, a getCause() to Twój wyjątek.
    //   void:              nie ma gdzie go oddać, więc Spring woła AsyncUncaughtExceptionHandler.
    //
    // Własną obsługę podajesz przez AsyncConfigurer (zobacz AsyncSupportConfig) — zostawia ona wykonawcę Boota
    // (test 1: wątek nadal nazywa się "task-N"). Domyślnie wyjątek z metody void jest tylko zalogowany (poziom ERROR).
    //
    // PUŁAPKA: @Async void, która „czasem nie działa” — bo rzuciła wyjątek, a nikt tego nie widzi. Dlaczego? Wywołujący
    //   dawno poszedł dalej, więc nie dostanie wyjątku; zostaje tylko wpis w logu. Wystawiaj metody zwracające
    //   CompletableFuture albo zdefiniuj AsyncUncaughtExceptionHandler z alertem/metryką.

    // =================================================================================================
    // 3. PUŁAPKI PROXY (test 5, 6, 7)
    // =================================================================================================
    //
    // a) Wywołanie z tej samej klasy (this.buildReport(...)) omija proxy — metoda wykonuje się W WĄTKU WYWOŁUJĄCYM
    //    (test 6: wynik to nazwa wątku testu, nie "task-N"). Przenieś metodę do osobnego beana.
    // b) @Async na metodzie prywatnej nie działa (test 7): proxy nie przechwyci metody prywatnej, a wywołanie
    //    z tej samej klasy i tak omija proxy. Metoda ma być publiczna (w osobnym beanie).
    // c) Zły typ zwracany (test 5): w starszych materiałach można przeczytać, że wywołujący dostaje null. Sprawdzone
    //    w Spring Framework 7.0.9: wywołanie rzuca IllegalArgumentException z komunikatem „Invalid return type for async
    //    method (only Future and void supported): class java.lang.String”.
    // d) Brak @EnableAsync = adnotacja jest ignorowana, metoda jedzie synchronicznie (jak w Cache01Cacheable z @EnableCaching).
    //
    // DOBRA PRAKTYKA: dla wywołujących oznaczaj asynchroniczność w typie zwracanym (CompletableFuture), a nie tylko
    //   adnotacją. Dlaczego? Sygnatura mówi „wynik przyjdzie później”, więc nikt nie traktuje wyniku jak gotowego,
    //   a wyjątki są częścią kontraktu.

    // =================================================================================================
    // 4. UTRATA KONTEKSTU WĄTKU (test 8, 10, 11)
    // =================================================================================================
    //
    // ThreadLocal jest związany z wątkiem. Zadanie @Async wykonuje inny wątek, więc NIE widzi:
    //   • MDC (np. requestId w logach) — test 8: w wątku puli MDC.get("requestId") to null;
    //   • kontekstu bezpieczeństwa zalogowanego użytkownika (Spring Security, lekcje s12) i transakcji (s10) —
    //     transakcja otwarta przez wywołującego NIE obejmuje metody @Async (to osobny wątek, osobne połączenie).
    //
    // Naprawa dla MDC: TaskDecorator (MdcTaskDecorator w tej lekcji) — kopiuje kontekst z wątku wysyłającego do
    // wykonującego i po zadaniu go sprząta. Wystarczy zadeklarować go jako bean: Boot nakłada beany TaskDecorator na
    // applicationTaskExecutor (test 10; test 11 sprawdza, że nic nie „przecieka” do następnego zadania).
    //
    // Nowość Boot 4.1: ustawienie spring.task.execution.propagate-context=true rejestruje ContextPropagatingTaskDecorator
    // ze Spring Frameworka (z jarów Boota 4.1.1), który przenosi kontekst przez bibliotekę Micrometer context-propagation.
    // Ta biblioteka NIE jest zależnością tego modułu (test 9), więc to tylko opis — nie uruchamiamy tego tutaj.
    //
    // PUŁAPKA: założenie, że „@Async zachowa moją transakcję/użytkownika”. Dlaczego? To inny wątek: zmiany z metody
    //   @Async mogą zostać zatwierdzone osobno albo przed zatwierdzeniem transakcji wywołującego, a bez kontekstu
    //   bezpieczeństwa metoda widzi „anonimowego”. Przekazuj dane w parametrach (id, nazwa użytkownika), nie w
    //   ThreadLocal, i zaczynaj nową transakcję w metodzie asynchronicznej.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @EnableAsync + @Async na publicznej metodzie beana; wywołanie przez proxy (nie z tej samej klasy).
     *   • Zwracaj void, Future albo CompletableFuture — inny typ = IllegalArgumentException (Spring 7.0.9).
     *   • Boot: applicationTaskExecutor = ThreadPoolTaskExecutor, rdzeń 8, kolejka bez limitu, prefiks "task-".
     *   • Własny bean Executor wyłącza executor Boota; ustawienia spring.task.execution.* przestają działać.
     *   • CompletableFuture: wyjątek w future (get → ExecutionException); void: AsyncUncaughtExceptionHandler.
     *   • MDC, SecurityContext i transakcja to ThreadLocal — nie przechodzą do wątku puli (TaskDecorator naprawia MDC).
     *   • Testy: CompletableFuture.get(limit) jako bezpiecznik albo Awaitility — nigdy asercja na czasie.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego @Async nie działa, gdy metoda woła inną metodę @Async tej samej klasy?
     *   2. Co się stanie? Wywołasz metodę z @Async, która zwraca String.
     *   3. ZNAJDŹ BŁĄD: metoda @Async void wysyłająca e-mail „czasem nie wysyła”, a w aplikacji nie widać żadnego błędu.
     *      Gdzie szukać i jak to uczynić widocznym?
     *   4. Co się stanie? W kontrolerze ustawiasz MDC.put("requestId", "REQ-123"), a metoda @Async loguje
     *      MDC.get("requestId") — co zostanie wypisane i jak to naprawić?
     *   5. ZNAJDŹ BŁĄD: ktoś dodał własny @Bean Executor, a spring.task.execution.pool.core-size=20 przestało działać.
     *      Dlaczego?
     *   6. Ile zadań @Async wykona domyślny wykonawca równolegle i co dzieje się z dziewiątym?
     *   7. Jak odebrać wyjątek z metody zwracającej CompletableFuture?
     *   8. Dlaczego @Async na metodzie prywatnej nie zadziała?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Async04AsyncExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć CompletableFuture z sumą {@code a + b}, policzoną na podanym wykonawcy.
     * Podpowiedź: {@code CompletableFuture.supplyAsync(dostawca, wykonawca)} (t21_concurrency/Concurrency05CompletableFuture).
     */
    static CompletableFuture<Integer> exercise1(Executor executor, int a, int b) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): połącz dwa future w jeden z sumą ich wyników. Gdy którykolwiek zakończy się wyjątkiem,
     * wynik też ma się zakończyć wyjątkiem (nie zgub błędu!). Nie wolno blokować (żadnego get/join w środku).
     * Podpowiedź: {@code thenCombine}.
     */
    static CompletableFuture<Integer> exercise2(CompletableFuture<Integer> first, CompletableFuture<Integer> second) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): w klasie ExerciseNotifier sprawić, żeby notifyCustomer wykonywała się w wątku puli
     * Boota (nazwa zaczyna się od "task-"), a nie w wątku wywołującym. Ta metoda jest tylko opisem — edytujesz
     * ExerciseNotifier. Podpowiedź: sekcja 1; klasa startowa lekcji ma już @EnableAsync.
     */
    static void exercise3() {
        // TODO: otwórz ExerciseNotifier i popraw ją (ta metoda jest tylko opisem — nic w niej nie pisz)
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Wywołanie this.metoda() idzie bezpośrednio do obiektu i omija proxy, a to proxy przekazuje zadanie do puli.
     *      Metoda wykona się w wątku wywołującym. Sprawdzone testem 6.
     *   2. Wywołanie rzuci IllegalArgumentException: „Invalid return type for async method (only Future and void
     *      supported): class java.lang.String”. Sprawdzone testem 5 (w Spring Framework 7.0.9).
     *   3. Wyjątek z metody void nie wraca do wywołującego; domyślnie jest tylko zalogowany. Szukaj w logach; uczyń go
     *      widocznym przez AsyncConfigurer z AsyncUncaughtExceptionHandler (alert/metryka) albo zwracaj
     *      CompletableFuture. Sprawdzone testem 4.
     *   4. Wypisze się „null” — MDC jest ThreadLocal i nie przechodzi do wątku puli (test 8). Naprawa: bean
     *      TaskDecorator kopiujący MDC (test 10) albo przekazanie requestId w parametrze.
     *   5. Autokonfiguracja Boota tworzy applicationTaskExecutor tylko, gdy nie ma innego beana Executor; własny bean
     *      go wyłącza, więc spring.task.execution.pool.* nie dotyczy już Twojego wykonawcy. Sprawdzone testem 12.
     *   6. Osiem (rdzeń puli). Dziewiąte czeka w kolejce, bo kolejka jest nieograniczona (pojemność Integer.MAX_VALUE),
     *      więc pula nie urośnie. Sprawdzone testem 2.
     *   7. future.get(...) rzuca ExecutionException, a getCause() zwraca oryginalny wyjątek (albo future.exceptionally /
     *      handle bez blokowania). Sprawdzone testem 3.
     *   8. Proxy nie przechwyci metody prywatnej, a wywołanie z tej samej klasy i tak omija proxy — metoda wykona się
     *      synchronicznie. Sprawdzone testem 7.
     */
    // </editor-fold>
}
