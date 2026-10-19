package s15_actuator_logging.obs05_observations;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * <pre>
 * TEMAT: Observation API — jedna obserwacja zamiast osobnych metryk, logów i śladów
 *        (observation = obserwacja; observe = obserwować; handler = obsługa zdarzeń; context = kontekst obserwacji;
 *        key value = para klucz-wartość; low/high cardinality = niska/wysoka kardynalność; tracing = śledzenie)
 *
 * W SKRÓCIE:
 *   W Obs03Metrics rejestrowałeś metryki ręcznie. Obserwacja (Observation) to krok wyżej: opisujesz JEDNO zdarzenie
 *   w kodzie („wysyłka zamówienia”) — z nazwą i kluczami — a to, co z niego powstanie, zależy od HANDLERÓW
 *   zarejestrowanych w aplikacji: czasomierz metryk (robi to Micrometer), ślad rozproszony (po dodaniu śledzenia),
 *   albo Twój własny handler. Kod aplikacji nie zmienia się, gdy dodasz kolejny system monitoringu.
 *
 * ANALOGIA: kamera monitoringu przy kasie.
 *   Kasjer obsługuje klienta (obserwowane zdarzenie). Jedna kamera (obserwacja) nagrywa wszystko, a z nagrania
 *   korzysta kilku „odbiorców” (handlerów): licznik klientów na godzinę (metryka), szef zmiany (ślad całej drogi
 *   klienta po sklepie) i ochrona (własny handler). Kasjer nie musi wiedzieć, ilu odbiorców jest.
 *
 * JAK TO DZIAŁA:
 *   1. Boot tworzy bean ObservationRegistry (rejestr obserwacji) i rejestruje w nim handlery: czasomierz metryk
 *      (gdy jest MeterRegistry), każdy TWÓJ bean typu ObservationHandler, a po dodaniu tracingu także handler śladów.
 *   2. W kodzie:
 *        Observation.createNotStarted("deliveries.ship", registry)
 *            .lowCardinalityKeyValue("carrier", carrier)     ← trafia do TAGÓW metryki (mało wartości)
 *            .highCardinalityKeyValue("orderId", orderId)    ← NIE do metryk; tylko do śladów i własnych handlerów
 *            .observe(() -> ... kod ...);                    ← start, wykonanie, stop; wyjątek: zapis i rzucenie dalej
 *   3. Cykl życia: onStart → (kod; ewentualnie onError) → onStop. Każdy handler dostaje te zdarzenia.
 *   4. Wynik w metrykach: Timer "deliveries.ship" z tagami carrier=... i error=none (albo nazwa klasy wyjątku).
 *   5. Boot sam obserwuje: żądania HTTP serwera (http.server.requests — ta sama metryka, którą widziałeś w
 *      Obs03Metrics!), wywołania klienta HTTP zbudowanego przez Boota (http.client.requests) i zadania @Scheduled.
 *   6. Wyłączanie po przedrostku nazwy: management.observations.enable.deliveries=false (wygrywa najdłuższe dopasowanie).
 *
 *   Wniosek: metryka http.server.requests z Obs03Metrics to właśnie OBSERWACJA. Dlatego dodanie tracingu daje ślady
 *   dla zapytań HTTP bez ruszania kodu kontrolerów — to zapowiedź s22_extras/Ext04Tracing.
 *
 * SŁÓWKA:
 *   observation = obserwacja; registry = rejestr; handler = obsługa zdarzeń cyklu życia; context = kontekst
 *   (dane jednej obserwacji); low cardinality = niska kardynalność (mało wartości; wchodzi do tagów metryk);
 *   high cardinality = wysoka kardynalność (wiele wartości; tylko ślady); span = odcinek śladu (jeden krok żądania);
 *   trace = ślad (cała droga żądania przez usługi); noop = „nic nie robi” (pusty rejestr);
 *   convention = konwencja nazewnicza kluczy (ustalone nazwy dla typowych zdarzeń).
 *
 * ZOBACZ TEŻ: s15_actuator_logging/Obs03Metrics (metryki ręczne i http.server.requests),
 *             s22_extras/Ext04Tracing (ślady rozproszone na tej samej obserwacji),
 *             s18_aop/Aop01Aspects (aspekty — na nich działa adnotacja @Observed),
 *             s03_boot_basics/Boot04Starters (dodawanie zależności przez startery),
 *             s16_cache_scheduling_async/Sched03Scheduled (zadania cykliczne, które Boot też obserwuje).
 * </pre>
 */
public final class Obs05Observations {

    private Obs05Observations() {
    }

    // =================================================================================================
    // 1. OBSERWACJA BEZ SPRINGA — TestObservationRegistry (testy w klasie głównej)
    // =================================================================================================
    //
    // DeliveryService dostaje ObservationRegistry przez konstruktor. W teście podajemy TestObservationRegistry —
    // rejestr z biblioteki micrometer-observation-test (jest w starterze actuator-test), który zapamiętuje obserwacje.
    // Asercje: nazwa "deliveries.ship", lowCardinalityKeyValue carrier=DHL, highCardinalityKeyValue orderId=ZAM-001,
    // obserwacja zatrzymana. Przy wyjątku: błąd zapisany w obserwacji, a wyjątek rzucony dalej (observe go nie połyka).
    //
    // ObservationRegistry.NOOP to rejestr „bez nikogo”: kod w observe(...) i tak się wykona — dlatego klasy z
    // obserwacjami da się bezpiecznie wywoływać bez konfiguracji (w testach jednostkowych, w skryptach).
    //
    // DOBRA PRAKTYKA: wstrzykuj ObservationRegistry konstruktorem i NIE twórz własnego. Dlaczego? Bean od Boota ma
    //   już podpięte handlery (metryki, ślady, Twoje); własny rejestr byłby pusty i obserwacje by „znikały”.

    // =================================================================================================
    // 2. NISKA I WYSOKA KARDYNALNOŚĆ KLUCZY
    // =================================================================================================
    //
    // lowCardinalityKeyValue → tag metryki (seria w pamięci i w monitoringu: ma być mało wartości — Obs03Metrics, sekcja 4).
    // highCardinalityKeyValue → tylko do śladów i handlerów; w metryce go NIE MA (test: timer ma tagi carrier i error,
    // a nie ma orderId). Dzięki temu możesz zapisać szczegół „które zamówienie” bez wysadzenia monitoringu.
    //
    // PUŁAPKA: identyfikator zamówienia jako lowCardinalityKeyValue. Dlaczego? To powtórka błędu z tagami metryk:
    //   każde zamówienie to nowa seria czasomierza. Wartość, która ma wiele różnych postaci, daj jako
    //   highCardinalityKeyValue.

    // =================================================================================================
    // 3. BOOT: OBSERWACJA → METRYKA, TWÓJ HANDLER, ŻĄDANIA HTTP (testy WithSpring)
    // =================================================================================================
    //
    // • Timer deliveries.ship z tagiem carrier (test observationBecomesTimer), tag error = "none".
    // • Przy wyjątku tag error = "IllegalArgumentException" (test errorBecomesTag) — błędy widać w metrykach
    //   bez pisania catch.
    // • RecordingObservationHandler to zwykły @Component implementujący ObservationHandler: Boot rejestruje go sam i
    //   dostaje zdarzenia w kolejności start, (error), stop (test customHandlerIsCalled).
    // • Ten sam handler widzi też „start:http.server.requests” — Boot obserwuje zapytania HTTP tym samym mechanizmem
    //   (test bootObservesHttpRequests; obserwacja żądania obejmuje obserwację wysyłki w środku).
    //
    // WYŁĄCZANIE: management.observations.enable.deliveries=false → kod się wykonuje, ale nie ma ani metryki, ani
    // wywołań handlera (test Disabled). Nazwę dopasowujesz po przedrostku (deliveries, http.server...), wygrywa
    // najdłuższe dopasowanie (ćwiczenie 3).
    //
    // Uwaga o wersjach: pakiety klas Boota dla obserwacji w 4.x to org.springframework.boot.micrometer.observation.*
    // (np. ObservationRegistryCustomizer); w starych tutorialach (Boot 2.x/3.x) znajdziesz org.springframework.boot.actuate.*.

    // =================================================================================================
    // 4. @Observed — ADNOTACJA (opis; w tym module bez testu)
    // =================================================================================================
    //
    // @Observed(name = "deliveries.ship") na metodzie beana robi obserwację bez pisania Observation.createNotStarted.
    // Działa przez aspekt (ObservedAspect), który Boot 4.1.1 tworzy tylko gdy: (1) na ścieżce klas jest AspectJ
    // (spring-boot-starter-aspectj), (2) włączono management.observations.annotations.enabled=true — domyślnie
    // jest false (sprawdzone w metadanych Boota). Ten moduł nie ma startera aspectj, więc adnotacji nie uruchamiamy;
    // zasady aspektów i proxy: s18_aop/Aop01Aspects.
    //
    // PUŁAPKA: @Observed na metodzie wywoływanej z TEJ SAMEJ klasy (this.metoda()). Dlaczego? Adnotacja działa przez
    //   proxy (pośrednika wokół beana); wywołanie wewnątrz klasy omija pośrednika — obserwacji nie będzie (ten sam
    //   mechanizm co w @Transactional: s10_transactions/Tx03ProxyPitfalls). Ręczne Observation.observe(...) tego
    //   problemu nie ma.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Observation.createNotStarted(nazwa, registry).lowCardinalityKeyValue(...).highCardinalityKeyValue(...).observe(...).
     *   • Low cardinality → tagi metryki; high cardinality → tylko ślady i własne handlery.
     *   • Handlery (metryki, ślady, Twój bean ObservationHandler) rejestruje Boot; zdarzenia: onStart, onError, onStop.
     *   • Wyjątek w observe(...) trafia do obserwacji (tag error w metryce) i jest rzucany dalej.
     *   • Boot obserwuje sam: http.server.requests, http.client.requests (klient zbudowany przez Boota), zadania @Scheduled.
     *   • Wyłączanie: management.observations.enable.<przedrostek>=false (najdłuższe dopasowanie wygrywa).
     *   • Test bez Springa: TestObservationRegistry + TestObservationRegistryAssert; ObservationRegistry.NOOP — bez handlerów.
     *   • @Observed: potrzebuje AspectJ i management.observations.annotations.enabled=true (domyślnie false).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się lowCardinalityKeyValue od highCardinalityKeyValue i gdzie trafia każdy z nich?
     *   2. Co się stanie? W observe(...) kod rzuca IllegalArgumentException. Czy wyjątek dojdzie do wywołującego i co
     *      będzie w tagu error metryki?
     *   3. Co się stanie? Ustawiasz management.observations.enable.deliveries=false i wołasz DeliveryService.ship("ZAM-1", "DHL").
     *      Czy dostaniesz numer przesyłki i czy powstanie metryka?
     *   4. ZNAJDŹ BŁĄD: .lowCardinalityKeyValue("orderId", orderId) — co z tego wyniknie po miesiącu działania?
     *   5. ZNAJDŹ BŁĄD: w serwisie napisano ObservationRegistry registry = ObservationRegistry.create(); zamiast wstrzyknąć
     *      bean. Dlaczego metryki z tego serwisu nie pojawiają się w /actuator/metrics?
     *   6. Co się stanie? Wysyłasz POST /api/deliveries?...; jakie obserwacje zobaczy RecordingObservationHandler?
     *   7. Dlaczego metryka http.server.requests z lekcji Obs03Metrics jest „obserwacją”?
     *   8. Kiedy zadziała adnotacja @Observed w tym projekcie i co trzeba w tym celu dodać?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Obs05ObservationsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): które klucze obserwacji staną się TAGAMI metryki? Zwróć posortowaną alfabetycznie listę:
     * wszystkie klucze niskiej kardynalności plus zawsze "error" (dokłada go czasomierz). Klucze wysokiej kardynalności
     * pomiń. Przykład: low {carrier=DHL}, high {orderId=ZAM-1} → ["carrier", "error"].
     * Podpowiedź: TreeSet, add, addAll (t12_collections/Collections04Sets).
     */
    static List<String> exercise1(Map<String, String> lowCardinality, Map<String, String> highCardinality) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): mała „obserwacja” z handlerem zapisującym zdarzenia. observe(nazwa, kod): zapisz
     * "start:nazwa", wykonaj kod; gdy rzuci wyjątek — zapisz "error:nazwa:NazwaKlasyWyjątku" (getSimpleName());
     * ZAWSZE na końcu zapisz "stop:nazwa"; wyjątek rzuć dalej, a wynik kodu zwróć. events() zwraca kopię zapisu.
     * Podpowiedź: try / catch (RuntimeException) / finally (t10_exceptions/Exceptions04TryWithResources).
     */
    static final class MiniObservation {

        <T> T observe(String name, Supplier<T> body) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        List<String> events() {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): czy obserwacja o danej nazwie jest włączona? Reguły to mapa „przedrostek → włączona”
     * (jak {@code management.observations.enable.<przedrostek>}). Przedrostek pasuje, gdy nazwa jest mu równa albo zaczyna się
     * od niego i kropki ("http" pasuje do "http.server.requests", ale NIE do "httpx.cos"). Wygrywa NAJDŁUŻSZY pasujący
     * przedrostek; gdy żaden nie pasuje — obserwacja jest włączona (true).
     * Przykład: {http=false, http.server=true}: "http.client.requests" → false; "http.server.requests" → true;
     * "deliveries.ship" → true.
     * Podpowiedź: pętla po entrySet i zapamiętanie najdłuższego pasującego klucza.
     */
    static boolean exercise3(Map<String, Boolean> rules, String observationName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Niska kardynalność (mało możliwych wartości, np. carrier) trafia do TAGÓW metryki; wysoka (wiele wartości,
     *      np. orderId) — tylko do śladów i własnych handlerów, NIE do metryk. Sprawdzone: observationBecomesTimer
     *      (tagi carrier i error, brak orderId).
     *   2. Tak — wyjątek zostaje rzucony dalej (observe go nie połyka), a w tagu error jest nazwa klasy wyjątku:
     *      "IllegalArgumentException". Sprawdzone: errorIsRecordedAndRethrown i errorBecomesTag.
     *   3. Dostaniesz numer ("TRK-..."), bo kod i tak się wykonuje, ale metryka deliveries.ship nie powstanie, a
     *      handler nie dostanie zdarzeń. Sprawdzone: Disabled.codeRunsButNothingIsObserved.
     *   4. Każde zamówienie to nowa wartość tagu, czyli nowa seria czasomierza — liczba serii rośnie bez końca
     *      (wysoka kardynalność, jak w Obs03Metrics). Takie dane dajesz jako highCardinalityKeyValue.
     *   5. Własny rejestr nie ma handlerów, które podpina Boot (metryki, ślady), więc obserwacje nigdzie nie
     *      „lądują”. Trzeba wstrzyknąć bean ObservationRegistry.
     *   6. Handler zobaczy start:http.server.requests, start:deliveries.ship, stop:deliveries.ship i
     *      stop:http.server.requests (obserwacja wysyłki zawiera się w obserwacji żądania). Sprawdzone testem
     *      bootObservesHttpRequests (sprawdza obecność zdarzeń obu obserwacji).
     *   7. Boot mierzy zapytania serwera przez Observation API (filtr żądań tworzy obserwację
     *      http.server.requests), a handler metryk zamienia ją w czasomierz z tagami method, status, uri...
     *      Dodanie handlera śladów dałoby z tej samej obserwacji także ślady. Sprawdzone: handler widzi
     *      http.server.requests.
     *   8. Gdy na ścieżce klas jest AspectJ (starter spring-boot-starter-aspectj) i ustawisz
     *      management.observations.annotations.enabled=true (domyślnie false). Ten moduł nie ma startera aspectj,
     *      więc bez zmiany pom.xml nie zadziała. (Wniosek z klas autokonfiguracji i metadanych Boota 4.1.1 —
     *      adnotacji w tym module nie uruchamiano.)
     */
    // </editor-fold>
}
