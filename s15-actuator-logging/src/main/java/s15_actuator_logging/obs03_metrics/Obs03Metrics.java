package s15_actuator_logging.obs03_metrics;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * TEMAT: Metryki — Micrometer: licznik, czasomierz, wskaźnik i tagi
 *        (metric = metryka, czyli mierzona liczba; meter = miernik; registry = rejestr; counter = licznik;
 *        timer = czasomierz; gauge = wskaźnik/manometr; tag = etykieta; cardinality = kardynalność, liczba różnych wartości)
 *
 * W SKRÓCIE:
 *   Logi mówią, CO się stało. Metryki mówią, ILE i JAK CZĘSTO: ile zamówień na godzinę, ile błędów, jak długo trwa
 *   przetwarzanie. Micrometer to biblioteka metryk wbudowana w Spring Boota (starter actuator ją dokłada). Piszesz
 *   do jednego interfejsu, MeterRegistry, a Boot może podłączyć dowolny system metryk (Prometheus, Datadog...).
 *   Bez takiego systemu metryki żyją w pamięci (SimpleMeterRegistry) i oglądasz je pod /actuator/metrics.
 *
 * ANALOGIA: licznik prądu, prędkościomierz i stoper w samochodzie.
 *   Licznik kilometrów (Counter) tylko rośnie. Prędkościomierz (Gauge) pokazuje wartość „teraz” — raz więcej,
 *   raz mniej. Stoper (Timer) mierzy, ile razy i jak długo trwały jazdy. Tag to „etykieta” na dane: osobny
 *   licznik kilometrów dla trasy „miasto” i „autostrada”. Gdy zrobisz etykietę „numer rejestracyjny kierowcy”
 *   dla każdego z miliona kierowców — pulpit nie zmieści się w żadnej szafie. To właśnie wysoka kardynalność.
 *
 * JAK TO DZIAŁA:
 *   1. Boot tworzy bean MeterRegistry i dodaje metryki JVM (jvm.memory.used...), systemu, Tomcata i HTTP
 *      (http.server.requests — o każdym zapytaniu: metoda, status, szablon adresu).
 *   2. Ty wstrzykujesz MeterRegistry i rejestrujesz własne metryki:
 *        Counter.builder("orders.created").tag("channel", "WEB").register(registry).increment();
 *        Timer.builder("orders.processing").register(registry).record(() -> ...);
 *        Gauge.builder("orders.open", openOrders, AtomicInteger::get).register(registry);
 *   3. Meter = nazwa + zestaw tagów. Ta sama nazwa z inną wartością tagu to NOWA, niezależna seria.
 *      register(...) dla istniejącej pary nazwa+tagi zwraca ten sam obiekt (nie tworzy drugiego).
 *   4. /actuator/metrics (po wystawieniu — Obs01Actuator) pokazuje nazwy; /actuator/metrics/orders.created
 *      pokazuje wartość i dostępne tagi; ?tag=channel:WEB zawęża do jednej serii (brak danych → 404).
 *   5. Wspólne tagi dla wszystkich metryk: management.metrics.tags.application=sklep.
 *
 * SŁÓWKA:
 *   meter registry = rejestr mierników; counter = licznik (tylko rośnie); timer = czasomierz (liczba i czas);
 *   gauge = wskaźnik (wartość chwilowa); tag = etykieta; series = seria (jedna kombinacja nazwa+tagi);
 *   high cardinality = wysoka kardynalność (bardzo wiele różnych wartości tagu); tag value = wartość tagu;
 *   scrape = „pobranie” metryk przez system monitoringu (np. Prometheus odpytuje aplikację co kilkanaście sekund).
 *
 * ZOBACZ TEŻ: s15_actuator_logging/Obs01Actuator (wystawianie punktu końcowego metrics),
 *             s15_actuator_logging/Obs05Observations (jedna obserwacja = metryka + ślad),
 *             s04_web_rest/Rest01Controllers (kontroler, którego zapytania Boot mierzy sam).
 * </pre>
 */
public final class Obs03Metrics {

    private Obs03Metrics() {
    }

    // =================================================================================================
    // 1. COUNTER — licznik zdarzeń
    // =================================================================================================
    //
    // Counter tylko rośnie (increment()). Reset następuje dopiero przy restarcie aplikacji; system metryk liczy
    // z niego tempo („zamówień na minutę”). W OrderService: Counter "orders.created" z tagiem channel.
    // Test 1: WEB ×2 i SKLEP ×1 → dwie serie, liczniki 2 i 1.
    //
    // DOBRA PRAKTYKA: nazywaj metryki małymi literami z kropkami (orders.created), a jednostkę dopisuj, gdy nie jest
    //   oczywista (np. orders.value.pln). Dlaczego? Micrometer tłumaczy kropki na konwencję każdego backendu
    //   (np. orders_created_total w Prometheusie) — spójna nazwa działa wszędzie.

    // =================================================================================================
    // 2. TIMER — ile razy i jak długo
    // =================================================================================================
    //
    // timer.record(() -> kod) uruchamia kod i zapisuje czas. Odczyt pokazuje COUNT (liczba wykonań), TOTAL_TIME
    // (łączny czas) i MAX. Test 2 sprawdza WYŁĄCZNIE count: czasu wykonania nie wolno asercjonować, bo zależy od
    // komputera (testy miałyby przypadkowo padać).
    //
    // DOBRA PRAKTYKA: mierz czasomierzem operacje, które mogą być wolne (zapytanie do bazy, wywołanie API) — nie
    //   każdą metodę. Dlaczego? Każdy Timer to osobna seria w pamięci i w monitoringu; wartość mają te, które
    //   odpowiadają na pytanie „co spowalnia sklep?”.

    // =================================================================================================
    // 3. GAUGE — wartość chwilowa
    // =================================================================================================
    //
    // Gauge nie zlicza zdarzeń — odczytuje wartość z obiektu przy każdym zbieraniu metryk. W OrderService:
    // openOrders (AtomicInteger) rośnie przy placeOrder i maleje przy shipOrder; Gauge to pokazuje (test 3).
    //
    // PUŁAPKA: Gauge zbudowany na obiekcie, którego nikt nie trzyma, np.
    //   Gauge.builder("kolejka", new AtomicInteger(5), AtomicInteger::get).register(registry);
    //   Dlaczego to błąd? Micrometer przechowuje obiekt tylko przez słabe odwołanie (WeakReference — nie blokuje
    //   odśmiecania pamięci), więc po pierwszym uruchomieniu garbage collectora (odśmiecacza) obiekt znika, a
    //   wskaźnik pokazuje NaN („nie liczba”). Trzymaj obiekt w polu beana, tak jak openOrders w OrderService.
    //   (Zachowanie opisane w dokumentacji Micrometera; nie testujemy go, bo zależy od momentu działania GC.)

    // =================================================================================================
    // 4. PUŁAPKA WYSOKIEJ KARDYNALNOŚCI
    // =================================================================================================
    //
    // Test 4: tag customerId dla 5 klientów → 5 serii, tag channel → 2 serie. Każda seria to pamięć w aplikacji
    // i koszt (pieniądze!) w systemie monitoringu — przy milionie klientów to milion serii, a system metryk potrafi
    // „zatkać się” albo wystawić rachunek.
    //
    // PUŁAPKA: identyfikator klienta, numer zamówienia, adres e-mail, pełny adres URL z id jako WARTOŚĆ TAGU.
    //   Dlaczego? Liczba serii = liczba różnych wartości tagu. Tagi mają mieć mały, z góry znany zbiór wartości
    //   (kanał, status, kategoria produktu). Szczegóły o konkretnym kliencie to zadanie dla LOGÓW i śladów.
    //   Boot stosuje tę zasadę sam: tag uri w http.server.requests to SZABLON /api/orders/{id}, a nie /api/orders/ZAM-100
    //   (test 5: dwa zapytania o różne id → jedna seria, liczba rośnie o 2).

    // =================================================================================================
    // 5. BOOT: REJESTR, /actuator/metrics, METRYKI HTTP
    // =================================================================================================
    //
    // • W module z samym starterem actuator, bez backendu monitoringu, bean MeterRegistry to SimpleMeterRegistry
    //   (sprawdzone testem — metryki istnieją tylko w pamięci i pod /actuator/metrics). Dodanie zależności
    //   micrometer-registry-prometheus (i wystawienie punktu prometheus) podłącza Prometheusa bez zmian w kodzie
    //   aplikacji — o to chodzi w Micrometerze (to nie jest przedmiotem tego modułu).
    // • Lista /actuator/metrics zawiera m.in. jvm.memory.used, http.server.requests oraz nasze orders.open i
    //   orders.processing (testy). Odczyt jednej: GET /actuator/metrics/orders.created?tag=channel:WEB.
    // • Wspólny tag: management.metrics.tags.application=sklep → availableTags każdej metryki zawiera application=sklep.
    // • Nieistniejąca nazwa albo tag bez danych → 404.
    //
    // Nie mieszaj z Obs05Observations: Observation API (Micrometer) potrafi jedną „obserwacją” wygenerować metrykę
    // i ślad naraz — tak działa http.server.requests; tu rejestrujemy metryki ręcznie.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • MeterRegistry wstrzykujesz konstruktorem; Counter (rośnie), Timer (liczba + czas), Gauge (wartość teraz).
     *   • Meter = nazwa + tagi; inna wartość tagu = nowa seria; register() dla tej samej pary zwraca ten sam obiekt.
     *   • Tagi: mały, znany zbiór wartości. NIGDY id klienta/zamówienia ani e-mail. Szczegóły → logi.
     *   • Gauge trzymaj w polu beana (słabe odwołanie!). Timer.record(...) mierzy operację; w testach liczymy count.
     *   • Test metryk własnych: new SimpleMeterRegistry() + registry.get(nazwa).tag(k, v).counter().count().
     *   • /actuator/metrics, /actuator/metrics/{nazwa}?tag=k:v; wspólne tagi: management.metrics.tags.<klucz>=<wartość>.
     *   • http.server.requests: tag uri = szablon adresu (/api/orders/{id}).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się Counter od Gauge? Który wybierzesz na „liczbę zalogowanych użytkowników teraz”?
     *   2. Co się stanie? W OrderService składasz dwa zamówienia z channel "WEB" i jedno z "SKLEP". Ile serii ma
     *      metryka orders.created i jakie wartości mają jej liczniki?
     *   3. Co się stanie? GET /actuator/metrics/orders.created?tag=channel:NIE-MA-TAKIEGO — jaki kod HTTP?
     *   4. ZNAJDŹ BŁĄD: Counter.builder("orders.by.customer").tag("customerId", id).register(registry) — dlaczego to
     *      wygląda niewinnie, a po miesiącu zatyka monitoring?
     *   5. ZNAJDŹ BŁĄD: Gauge.builder("kolejka", new AtomicInteger(5), AtomicInteger::get).register(registry) —
     *      czemu po jakimś czasie wskaźnik pokazuje NaN?
     *   6. Co się stanie? Wysyłasz GET /api/orders/ZAM-100 i GET /api/orders/ZAM-101. Ile osobnych serii (po tagu uri) w
     *      http.server.requests utworzą te dwa zapytania i jaką wartość ma tag uri?
     *   7. Dlaczego w testach metryk nie sprawdzamy TOTAL_TIME ani czasu Timera?
     *   8. Który bean MeterRegistry dostajesz w module bez żadnego backendu monitoringu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Obs03MetricsExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zbuduj nazwę metryki w konwencji Micrometera: obszar i zdarzenie małymi literami,
     * rozdzielone kropką, z pominięciem spacji na brzegach. ("Orders", " Created ") → "orders.created".
     * Pusty (lub złożony ze spacji) obszar albo zdarzenie → IllegalArgumentException.
     * Podpowiedź: strip(), toLowerCase(), isBlank().
     */
    static String exercise1(String area, String event) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): mały model „serii” — jak Micrometer rozróżnia serie po nazwie i tagach.
     * increment(nazwa, tagi) zwiększa licznik serii o 1; count(nazwa, tagi) zwraca wartość (0, gdy serii nie ma);
     * seriesCount(nazwa) zwraca liczbę RÓŻNYCH serii o tej nazwie. Kolejność tagów w mapie nie ma znaczenia:
     * {a=1, b=2} i {b=2, a=1} to ta sama seria.
     * Podpowiedź: kluczem w Map może być tekst zbudowany z nazwy i posortowanych tagów (TreeMap).
     */
    static final class TagCounters {

        void increment(String name, Map<String, String> tags) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        long count(String name, Map<String, String> tags) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }

        int seriesCount(String name) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wykryj tagi o zbyt wysokiej kardynalności. Dla mapy „nazwa tagu → zaobserwowane
     * wartości” zwróć posortowaną alfabetycznie listę nazw tagów, które mają WIĘCEJ różnych wartości niż limit.
     * Przykład: {channel={WEB,SKLEP}, customerId={1,2,3,4,5,6}}, limit 5 → ["customerId"].
     * Ujemny limit → IllegalArgumentException.
     * Podpowiedź: entrySet().stream(), filter po value().size(), sorted (t13_lambdas/Lambda03JavaUtilFunction).
     */
    static List<String> exercise3(Map<String, Set<String>> observedValues, int limit) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Counter tylko rośnie (zdarzenia: złożone zamówienia, błędy); Gauge pokazuje wartość chwilową, która może
     *      rosnąć i maleć. „Zalogowani teraz” → Gauge (wartość bywa mniejsza niż przed chwilą). Test 3 pokazuje Gauge.
     *   2. Dwie serie (channel=WEB i channel=SKLEP); liczniki: WEB = 2, SKLEP = 1. Sprawdzone testem 1.
     *   3. 404 Not Found — dla tagu bez danych punkt końcowy nie ma czego pokazać. Sprawdzone: WithSpring.ownMetricInEndpoint.
     *   4. Każdy nowy klient to nowa wartość tagu, czyli nowa seria: liczba serii rośnie bez końca razem z bazą klientów.
     *      Test 4: 5 klientów → 5 serii, a tag channel daje stałe 2. Szczegóły o kliencie należą do logów.
     *   5. Micrometer trzyma obiekt wskaźnika tylko słabym odwołaniem; nikt inny go nie trzyma, więc po pierwszym GC
     *      obiekt znika, a wskaźnik pokazuje NaN. Obiekt trzymaj w polu beana. (Z dokumentacji Micrometera — w teście
     *      nie odtwarzamy, bo moment GC jest niedeterministyczny.)
     *   6. Jedną serię: obie wartości trafiają do tej samej serii z uri=/api/orders/{id}, a liczba
     *      zapytań w tej serii rośnie o 2. Sprawdzone: WithSpring.httpMetricUsesUriTemplate.
     *   7. Czas zależy od komputera, obciążenia i JIT-a, więc asercja miałaby przypadkowo padać. Sprawdzamy count
     *      (deterministyczny) — że pomiar w ogóle nastąpił.
     *   8. SimpleMeterRegistry (rejestr w pamięci). Sprawdzone: WithSpring.registryIsInMemory.
     */
    // </editor-fold>
}
