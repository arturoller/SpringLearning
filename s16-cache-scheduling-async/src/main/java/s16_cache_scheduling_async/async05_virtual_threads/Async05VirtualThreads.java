package s16_cache_scheduling_async.async05_virtual_threads;

import java.util.List;

/**
 * <pre>
 * TEMAT: Wątki wirtualne w Spring Boot — spring.threads.virtual.enabled
 *        (virtual thread = wątek wirtualny, lekki wątek zarządzany przez JVM; platform thread = wątek platformowy,
 *         zwykły wątek systemu operacyjnego; carrier thread = wątek nośny; pinning = przypięcie wątku do nośnika)
 *
 * W SKRÓCIE:
 *   Klasyczny serwer daje każdemu żądaniu osobny wątek systemowy z ograniczonej puli (Tomcat: domyślnie 200). Gdy żądania
 *   głównie CZEKAJĄ (na bazę, cudze API), pula się wyczerpuje, choć procesor się nudzi. Wątek wirtualny (Java 21)
 *   kosztuje tyle co mały obiekt, więc możesz mieć ich dziesiątki tysięcy, a blokada na I/O odkłada go na bok i oddaje
 *   nośnik innemu. W Spring Boot wystarczy jedno ustawienie: spring.threads.virtual.enabled=true — kod się nie zmienia.
 *   To NIE jest przyspieszacz obliczeń: pomaga, gdy jest dużo czekania, nie gdy jest dużo liczenia.
 *
 * ANALOGIA: restauracja z kelnerami.
 *   Wątek platformowy = kelner na etacie (drogi, jest ich kilku). Zamówienie obsługuje jeden kelner od początku do
 *   końca, więc stoi przy kuchni i czeka na danie. Wątek wirtualny = kelner „na zawołanie”: gdy danie nie jest gotowe,
 *   odkłada zamówienie i obsługuje innego gościa; wraca, gdy kuchnia zawoła. Kuchnia (baza danych z ograniczoną pulą
 *   połączeń) nadal ma tylko kilka palników — więcej kelnerów nie ugotuje szybciej.
 *
 * JAK TO DZIAŁA:
 *   Boot 4.1.1 — co zmienia spring.threads.virtual.enabled=true (sprawdzone testami 1–5):
 *     • Tomcat: bean tomcatVirtualThreadsProtocolHandlerCustomizer; żądania idą na wątki wirtualne "tomcat-handler-N";
 *       server.tomcat.threads.max (domyślnie 200) przestaje mieć znaczenie (opis w metadanych Boota).
 *     • @Async: applicationTaskExecutor to SimpleAsyncTaskExecutor (nowy wątek wirtualny na zadanie, prefiks "task-"),
 *       a nie ThreadPoolTaskExecutor.
 *     • @Scheduled: taskScheduler to SimpleAsyncTaskScheduler (zadania na wątkach wirtualnych, prefiks "scheduling-").
 *     • Ustawienia spring.task.execution.pool.* i spring.task.scheduling.pool.* przestają mieć znaczenie (opis w
 *       metadanych Boota: „Doesn't have an effect if virtual threads are enabled”).
 *   Domyślnie spring.threads.virtual.enabled=false (test 4–5: wątki platformowe, pule jak w lekcji Async04Async).
 *   Pod spodem JVM uruchamia wątki wirtualne na kilku wątkach nośnych (carrier); blokująca operacja wątku wirtualnego
 *   zdejmuje go z nośnika (unmount), a po jej zakończeniu wątek wraca na dowolny nośnik.
 *
 * SŁÓWKA:
 *   virtual thread = wątek wirtualny; platform thread = wątek platformowy; carrier = nośnik; mount/unmount =
 *   zamontować/zdemontować (przypisać wątek wirtualny do nośnika albo go zdjąć); pinning = przypięcie (wątek wirtualny
 *   nie może zostać zdjęty z nośnika podczas blokady); blocking = blokowanie (czekanie); I/O-bound = ograniczone
 *   czekaniem na wejście/wyjście; CPU-bound = ograniczone mocą procesora; connection pool = pula połączeń.
 *
 * ZOBACZ TEŻ: s16_cache_scheduling_async/Async04Async (pule wątków i @Async),
 *             s16_cache_scheduling_async/Sched03Scheduled (harmonogram),
 *             t21_concurrency/Concurrency01Threads, t21_concurrency/Concurrency03Locks (synchronized i ReentrantLock),
 *             t21_concurrency/Concurrency04Executors (pule wątków), t21_concurrency/Concurrency10MemoryModel.
 * </pre>
 */
public final class Async05VirtualThreads {

    private Async05VirtualThreads() {
    }

    // =================================================================================================
    // 1. WŁĄCZENIE I EFEKT W BOOT 4.1 (test 1–5)
    // =================================================================================================
    //
    //   spring.threads.virtual.enabled=true            (application.properties; wymaga Javy 21+)
    //
    // Efekt widać w teście na prawdziwym porcie (RANDOM_PORT): kontroler dostaje ThreadInfo(name, virtual) i
    // Thread.currentThread().isVirtual() daje true, a nazwa zaczyna się od "tomcat-handler-". Metoda @Async: ten sam
    // test, nazwa "task-...". Z ustawieniem false (domyślnym): "http-nio-..." i pule ThreadPool*.
    //
    // Lekcja uruchamia aplikację z main z włączonymi wątkami wirtualnymi (ustawienie domyślne aplikacji), a testy
    // podają ustawienie jawnie — dzięki temu wynik nie zależy od tego, jak uruchomiono aplikację.
    //
    // DOBRA PRAKTYKA: najpierw zmierz, potem włączaj. Dlaczego? Zysk jest duży przy wielu równoległych żądaniach
    //   czekających na I/O, a przy kilku użytkownikach lub pracy głównie obliczeniowej — żaden.

    // =================================================================================================
    // 2. KIEDY POMAGAJĄ, A KIEDY NIE (test 7)
    // =================================================================================================
    //
    //   POMAGAJĄ: wiele równoległych zadań, które głównie czekają — zapytania do bazy i cudzych API, odczyt plików,
    //   kolejki. Test 7: 10 000 zadań czeka jednocześnie (żadne nie kończy się, dopóki wszystkie nie wystartują).
    //   Pula 8 wątków platformowych utknęłaby na ósmym zadaniu (osiem blokad zajmuje wszystkie wątki), z wątkami
    //   wirtualnymi test przechodzi.
    //
    //   NIE POMAGAJĄ: zadania liczące (CPU-bound — szyfrowanie, kompresja, obliczenia). Tu ogranicza Cię liczba
    //   rdzeni procesora, nie liczba wątków; tysiąc wątków wirtualnych liczących dzieli te same rdzenie.
    //
    // PUŁAPKA: pule wątków wirtualnych. Dlaczego? Wątek wirtualny jest tani, więc NIE tworzymy ich puli — jedno zadanie =
    //   jeden nowy wątek (Executors.newVirtualThreadPerTaskExecutor()). Pula służyłaby tylko do ograniczania
    //   współbieżności, a do tego jest Semaphore (patrz niżej).
    //
    // PUŁAPKA: ThreadLocal z dużymi obiektami. Dlaczego? Wątków wirtualnych są tysiące i każdy ma własną kopię
    //   ThreadLocal — pamięć rośnie. Przekazuj dane w parametrach; w Javie 25 do przekazywania kontekstu służy też
    //   ScopedValue (w Javie 21 to jeszcze funkcja podglądowa).

    // =================================================================================================
    // 3. OGRANICZONE ZASOBY NADAL OGRANICZAJĄ (test 8)
    // =================================================================================================
    //
    // 500 żądań na wątkach wirtualnych, które potrzebują połączenia z bazy, a pula połączeń ma 10 sztuk (domyślny
    // HikariCP w Boot ma 10 — wartość z dokumentacji Boota; moduł s16 nie ma JDBC, więc tu symulujemy ją Semaphore),
    // to nadal 10 naraz; pozostałe czekają na wolne połączenie. Test 8: maksymalna liczba jednocześnie „pracujących”
    // wynosi co najwyżej 10, choć wątków wirtualnych jest 500.
    //
    // PUŁAPKA: włączenie wątków wirtualnych „na ratunek” wolnej bazie. Dlaczego? Wątki wirtualne usuwają limit liczby
    //   wątków, ale nie limit zasobu: kolejka przed bazą się wydłuża, a nie skraca. Zwiększanie puli połączeń ponad
    //   to, co baza znosi, kończy się przeciążeniem bazy. Zasób ograniczony świadomie ogranicz Semaphore'em.

    // =================================================================================================
    // 4. PRZYPINANIE (PINNING) — JDK 21 A JDK 25
    // =================================================================================================
    //
    // Wątek wirtualny „przypięty” nie może zostać zdjęty z nośnika podczas blokady — blokuje wtedy cały wątek nośny,
    // a nośników jest tyle co rdzeni procesora.
    //   • JDK 21: blokująca operacja (sleep, czekanie na I/O, await) WEWNĄTRZ bloku {@code synchronized} przypina
    //     wątek wirtualny do nośnika. Przy kilku takich wątkach wszystkie nośniki mogą stanąć (aplikacja „zamarza”).
    //     Obejście w Javie 21: w kodzie, który blokuje, używaj ReentrantLock zamiast synchronized.
    //   • JDK 24 i nowsze (JEP 491 „Synchronize Virtual Threads without Pinning”, więc także JDK 25 LTS):
    //     synchronized już NIE przypina. Sprawdzone uruchomieniem na JDK 25: wątek wirtualny blokujący się wewnątrz
    //     synchronized nie zatrzymuje nośnika — przy jednym nośniku (-Djdk.virtualThreadScheduler.parallelism=1)
    //     inny wątek wirtualny dostał nośnik i wykonał się. Zachowania JDK 21 nie uruchamiałem w tym środowisku
    //     (jest tu tylko JDK 25) — opis pochodzi z JEP 491 i dokumentacji JDK 21.
    //   • Przypięcie zostaje przy wywołaniach metod natywnych (JNI) i funkcji zewnętrznych — krótkie, ale blokujące
    //     wywołanie natywne nadal trzyma nośnik. Do wykrywania przypięć służy zdarzenie JFR jdk.VirtualThreadPinned
    //     (zdarzenie istnieje w JDK 25).
    //
    // DOBRA PRAKTYKA: na Javie 21 przejrzyj biblioteki i własny kod pod kątem blokad wewnątrz synchronized; na
    //   Javie 25 problem w większości znika, więc to kolejny powód, by uczyć się i wdrażać na 25 (CLAUDE.md: zalecana).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • spring.threads.virtual.enabled=true (domyślnie false; Java 21+) — bez zmian w kodzie.
     *   • Boot 4.1.1: Tomcat obsługuje żądania na wątkach wirtualnych, applicationTaskExecutor = SimpleAsyncTaskExecutor,
     *     taskScheduler = SimpleAsyncTaskScheduler; pool.* przestają działać.
     *   • Pomagają przy I/O-bound (dużo czekania); nie pomagają przy CPU-bound.
     *   • Nie twórz puli wątków wirtualnych: jedno zadanie = jeden wątek; ogranicz zasób Semaphore'em.
     *   • Pula połączeń z bazą nadal ogranicza współbieżność.
     *   • Pinning: JDK 21 — synchronized + blokada przypina; JDK 24+ (JEP 491, w tym 25) — już nie; natywne wywołania nadal.
     *   • Testuj na prawdziwym porcie: MockMvc wykonuje kontroler w wątku testu, a nie Tomcata.
     *
     * PYTANIA KONTROLNE:
     *   1. Co zmienia spring.threads.virtual.enabled=true w Tomcacie, @Async i @Scheduled w Boot 4.1?
     *   2. Co się stanie? Ustawisz spring.threads.virtual.enabled=true i spring.task.execution.pool.core-size=2 — ile
     *      wątków będzie miała pula @Async?
     *   3. Kiedy wątki wirtualne przyspieszą aplikację, a kiedy nie? Podaj po jednym przykładzie.
     *   4. ZNAJDŹ BŁĄD: po włączeniu wątków wirtualnych aplikacja nadal „zamiera” przy 10 równoległych zapytaniach do
     *      bazy; zespół dodaje kolejne wątki. Dlaczego to nie pomoże?
     *   5. Co się stanie? Na JDK 21 wątek wirtualny czeka na I/O wewnątrz synchronized — a na JDK 25?
     *   6. ZNAJDŹ BŁĄD: ktoś tworzy FixedThreadPool(100) wątków wirtualnych, „żeby ograniczyć współbieżność”. Co jest
     *      nie tak i jak zrobić to dobrze?
     *   7. Dlaczego test wątku kontrolera robimy na RANDOM_PORT, a nie MockMvc?
     *   8. Który wątek zobaczysz w kontrolerze bez wątków wirtualnych (nazwa) i z nimi?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Async05VirtualThreadsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): uruchom krótkie zadanie na wątku wirtualnym i zwróć to, co {@code Thread.currentThread()
     * .isVirtual()} pokazało WEWNĄTRZ niego. Poczekaj na zakończenie wątku.
     * Podpowiedź: {@code Thread.ofVirtual().start(...)} albo {@code Thread.startVirtualThread}, potem {@code join()}.
     */
    static boolean exercise1() throws InterruptedException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): policz kwadraty liczb 0..n-1, każdy na OSOBNYM wątku wirtualnym, i zwróć je w kolejności
     * argumentów (0, 1, 4, 9...). Nie używaj puli o stałym rozmiarze.
     * Podpowiedź: {@code Executors.newVirtualThreadPerTaskExecutor()} w try-with-resources i lista {@code Future}.
     */
    static List<Integer> exercise2(int n) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): uruchom {@code tasks} razy {@code work} na wątkach wirtualnych, ale tak, by NARAZ
     * pracowało najwyżej {@code permits} zadań. Zwróć największą zaobserwowaną liczbę zadań pracujących jednocześnie.
     * Wszystkie zadania mają się wykonać (metoda wraca po ich zakończeniu).
     * Podpowiedź: {@code Semaphore} (acquire/release w finally) i dwa liczniki {@code AtomicInteger}
     * (bieżący i maksymalny); zobacz test 8 lekcji.
     */
    static int exercise3(int tasks, int permits, Runnable work) throws Exception {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tomcat obsługuje żądania na wątkach wirtualnych (bean tomcatVirtualThreadsProtocolHandlerCustomizer,
     *      wątki "tomcat-handler-N"); applicationTaskExecutor dla @Async to SimpleAsyncTaskExecutor (nowy wątek
     *      wirtualny na zadanie); taskScheduler dla @Scheduled to SimpleAsyncTaskScheduler. Sprawdzone testami 1–3.
     *   2. Żadnego limitu puli: nie ma puli. applicationTaskExecutor nie jest ThreadPoolTaskExecutor, a core-size jest
     *      ignorowane (opis w metadanych Boota i test 3).
     *   3. Pomagają: wiele równoległych żądań czekających na bazę lub cudze API. Nie pomagają: obliczenia (np.
     *      kompresja, szyfrowanie) — ogranicza je liczba rdzeni. Test 7 pokazuje pierwszy przypadek.
     *   4. Ograniczeniem jest pula połączeń z bazą (10), a nie liczba wątków; dodatkowe wątki tylko dłużej czekają
     *      w kolejce po połączenie. Test 8 pokazuje, że 500 wątków wirtualnych nadal pracuje „po 10 naraz”.
     *   5. JDK 21: wątek jest przypięty do nośnika na czas blokady — przy kilku takich wątkach nośniki mogą się
     *      wyczerpać. JDK 25 (od JDK 24, JEP 491): synchronized już nie przypina, nośnik zostaje oddany innym wątkom
     *      (sprawdzone uruchomieniem na JDK 25; zachowania JDK 21 nie uruchamiałem tu, opis z JEP 491).
     *   6. Pula wątków wirtualnych nie ma sensu: wątek jest tani, a pula nie ogranicza zasobu, tylko liczbę wątków.
     *      Zrób: newVirtualThreadPerTaskExecutor() i Semaphore na zasób, który naprawdę jest ograniczony (ćwiczenie 3).
     *   7. MockMvc woła kontroler w wątku testu (zwykły wątek platformowy), więc nie pokaże wątku Tomcata — potrzebny
     *      jest prawdziwy serwer (RANDOM_PORT).
     *   8. Bez wątków wirtualnych: platformowy, nazwa zaczyna się od "http-nio-" (test 4); z nimi: wirtualny,
     *      "tomcat-handler-" (test 1).
     */
    // </editor-fold>
}
