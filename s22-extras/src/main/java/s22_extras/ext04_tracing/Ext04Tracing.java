package s22_extras.ext04_tracing;

import java.util.List;

/**
 * <pre>
 * TEMAT: Śledzenie rozproszone (distributed tracing) — ślad i odcinek, propagacja nagłówka traceparent,
 *        identyfikator śladu w logach, eksport OpenTelemetry, próbkowanie, eksporter w pamięci w testach
 *        (trace = ślad; span = odcinek; OpenTelemetry = otwarty standard i biblioteki do śladów, metryk i logów)
 *
 * W SKRÓCIE:
 *   • Ślad to cała droga JEDNEGO zapytania przez wszystkie usługi. Składa się z odcinków: „serwer sklepu obsługuje
 *     zapytanie”, „sklep woła magazyn”, „magazyn obsługuje zapytanie”. Odcinki tworzą drzewo (rodzic → dzieci).
 *   • Usługi przekazują sobie ślad w nagłówku HTTP traceparent: 00-identyfikatorŚladu-identyfikatorRodzica-flagi.
 *   • Boot 4: starter spring-boot-starter-opentelemetry — Spring MVC, RestClient (z buildera Boota) i Spring Security
 *     same tworzą odcinki i przekazują nagłówek; w logach pojawia się [traceId-spanId].
 *   • Eksport (OTLP) do Jaegera/Tempo włącza JEDNA właściwość z adresem. Próbkowanie decyduje, ile śladów zapisać
 *     (domyślnie 10%).
 *   • W testach Boot 4 domyślnie wyłącza eksport i propagację; @AutoConfigureTracing włącza je z powrotem,
 *     a InMemorySpanExporter zbiera odcinki w pamięci.
 *
 * ANALOGIA: list polecony z numerem nadania.
 *   Poczta nadaje przesyłce numer (traceId). Każdy punkt — okienko, sortownia, listonosz — wpisuje do systemu „byłem
 *   tu od 10:02 do 10:05” (odcinek) i przekazuje dalej przesyłkę RAZEM z numerem (traceparent). Po numerze widać całą
 *   drogę i gdzie przesyłka utknęła. Jeśli sortownia zgubi numer (klient HTTP bez śledzenia), od niej zaczyna się
 *   „nowa przesyłka” i nie da się już połączyć historii.
 *
 * JAK TO DZIAŁA:
 *   GET /api/shop/ELE-001/availability                       (drzewo sprawdzone testem i w Jaegerze)
 *   [SERVER]   http get /api/shop/{sku}/availability          ← obserwacja Spring MVC, korzeń śladu (rodzic 000...0)
 *     [INTERNAL] security filterchain before / authorize request / secured request / ...after   ← Spring Security
 *       [INTERNAL] sprawdz-dostepnosc                         ← nasza Observation (AvailabilityService)
 *         [CLIENT]   http get                                  ← RestClient z buildera Boota; wysyła traceparent
 *           [SERVER]   http get /api/stock/{sku}               ← „magazyn”: przyjął traceparent, jest dzieckiem CLIENT
 *   Każdy odcinek: nazwa, rodzaj (kind), start, czas trwania, rodzic, atrybuty (np. http.url, status, sklep.sku).
 *   Pod spodem: Micrometer Observation → most micrometer-tracing-bridge-otel → OpenTelemetry SDK (SdkTracerProvider)
 *   → BatchSpanProcessor → eksportery (OtlpHttpSpanExporter, gdy podasz adres; w teście InMemorySpanExporter).
 *
 * SŁÓWKA:
 *   trace id = identyfikator śladu (32 znaki szesnastkowe); span id = identyfikator odcinka (16 znaków);
 *   parent = rodzic; root span = odcinek główny (korzeń); propagation = propagacja (przekazywanie kontekstu);
 *   sampling = próbkowanie; exporter = eksporter (wysyła odcinki na zewnątrz); collector = zbieracz;
 *   MDC (Mapped Diagnostic Context) = mapa wartości dołączanych do każdej linii logu w danym wątku;
 *   cardinality = liczność (ile różnych wartości może mieć atrybut).
 *
 * ZOBACZ TEŻ: s15_actuator_logging/Obs05Observations (Observation API), s15_actuator_logging/Obs04Logging (format
 *             logów), s15_actuator_logging/Obs03Metrics (metryki), s13_rest_client/Rc01RestClient (RestClient.Builder),
 *             s22_extras/Ext02Messaging (Kafka też umie przekazać ślad w nagłówkach wiadomości — po włączeniu obserwacji).
 * </pre>
 */
public final class Ext04Tracing {

    private Ext04Tracing() {
    }

    // =================================================================================================
    // 1. KIEDY PO TO SIĘGNĄĆ
    // =================================================================================================
    //
    // • Kilka usług woła się nawzajem i trzeba odpowiedzieć: „dlaczego to zapytanie trwało 3 sekundy?” albo „w której
    //   usłudze był błąd?”. Metryki (s15_actuator_logging/Obs03Metrics) mówią, ŻE jest wolno; ślad pokazuje, GDZIE.
    // • Logi wielu usług trzeba połączyć w jedną historię — wystarczy wyszukać traceId we wszystkich logach.
    // Nawet w jednej aplikacji identyfikator śladu w logach się przydaje: łączy wszystkie linie jednego zapytania,
    // gdy równolegle obsługiwanych jest ich sto.
    //
    // DOBRA PRAKTYKA: zacznij od samego identyfikatora śladu w logach (darmowe, zero infrastruktury), a eksport do
    //   Jaegera/Tempo dodaj, gdy usług jest kilka. Dlaczego? Serwer śladów to kolejny system do utrzymania i opłacenia.

    // =================================================================================================
    // 2. ŚLAD, ODCINEK, DRZEWO — CO TWORZY BOOT (Ext04TracingTest)
    // =================================================================================================
    //
    // Starter spring-boot-starter-opentelemetry (Boot 4) przynosi: Micrometer Tracing z mostem do OpenTelemetry,
    // OpenTelemetry SDK i eksporter OTLP. Boot tworzy beany m.in.: otelSdkTracerProvider (SdkTracerProvider),
    // otelSampler (ParentBasedSampler), otelSpanProcessor (BatchSpanProcessor), micrometerOtelTracer (Tracer
    // z Micrometera — tego używasz w kodzie). Odcinki powstają z OBSERWACJI: Spring MVC (serwer), RestClient (klient),
    // Spring Security (filtry, autoryzacja), Spring AI (wywołania modelu). Kafka — dopiero po włączeniu
    // spring.kafka.template.observation-enabled i spring.kafka.listener.observation-enabled (domyślnie false).
    //
    // Rodzaje odcinków (kind): SERVER — obsługa przychodzącego zapytania; CLIENT — wywołanie innej usługi;
    // INTERNAL — praca wewnątrz (nasza obserwacja, Security); są też PRODUCER/CONSUMER dla wiadomości.
    //
    // PUŁAPKA: szum w śladach. Spring Security dokłada do KAŻDEGO zapytania 4 odcinki („security filterchain before”,
    //   „authorize request”, „secured request”, „security filterchain after” — sprawdzone testem i w Jaegerze).
    //   Dlaczego ważne? Przy dużym ruchu to większość zapisanych danych (koszt). Spring Security pozwala je ograniczyć
    //   (bean SecurityObservationSettings — dokumentacja Spring Security, rozdział Observability).

    // =================================================================================================
    // 3. PROPAGACJA — NAGŁÓWEK traceparent (W3C Trace Context)
    // =================================================================================================
    //
    // Wartość, którą dostał „magazyn” (sprawdzone testem):
    //   traceparent: 00-8107e1277d9c0705af5c4f03ac7b198b-57f40112a07e7c4d-03
    //                ^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^ ^^
    //                │  identyfikator śladu (traceId)    odcinek RODZICA   flagi: bit 01 = „zapisz” (sampled),
    //                wersja formatu                      (tu: CLIENT)      bit 02 = traceId losowy → 03 = oba
    // Usługa, która dostaje traceparent, NIE zaczyna nowego śladu — jej odcinek SERVER ma za rodzica odcinek CLIENT.
    // Boot domyślnie WYSYŁA format W3C, a przyjmuje W3C i B3 (starszy format Zipkina) — management.tracing.propagation.*.
    //
    // PUŁAPKA: klient HTTP bez śledzenia. RestClient.create(), new RestTemplate() albo własny HttpClient nie mają
    //   obserwacji — nie wysyłają traceparent, więc magazyn zaczyna NOWY ślad (sprawdzone testem: RestClient.create()
    //   → receivedTraceparent = null, RestClient z RestClient.Builder Boota → nagłówek jest). Dlaczego ważne? Ślad
    //   „urywa się” bez żadnego błędu. Zawsze buduj klientów z beanów RestClient.Builder / WebClient.Builder.

    // =================================================================================================
    // 4. WŁASNE ODCINKI — Observation API (AvailabilityService)
    // =================================================================================================
    //
    //   Observation.createNotStarted("sklep.dostepnosc", observationRegistry)
    //           .contextualName("sprawdz-dostepnosc")                       ← nazwa odcinka
    //           .lowCardinalityKeyValue("sklep.kategoria", "ELE")           ← mało wartości: atrybut + tag METRYKI
    //           .highCardinalityKeyValue("sklep.sku", "ELE-001")            ← dużo wartości: tylko atrybut ODCINKA
    //           .observe(() -> restClient.get()...);
    // Jedna obserwacja = odcinek w śladzie + metryka czasu (s15_actuator_logging/Obs05Observations). Wywołanie HTTP
    // w środku staje się dzieckiem tego odcinka. Bieżący odcinek odczytasz z Tracer (ShopController):
    // tracer.currentSpan().context().traceId().
    //
    // PUŁAPKA: identyfikator zamówienia albo e-mail jako lowCardinalityKeyValue. Dlaczego źle? Każda nowa wartość tworzy
    //   nową serię metryk — tysiące serii zapychają pamięć i system metryk. Takie dane: highCardinalityKeyValue.

    // =================================================================================================
    // 5. IDENTYFIKATOR ŚLADU W LOGACH
    // =================================================================================================
    //
    // Linie logu z uruchomienia (obie „usługi”, TEN SAM traceId, różne spanId):
    //   INFO ... [o-auto-1-exec-1] [8107e1277d9c0705af5c4f03ac7b198b-159ad697e89c54e6] ...AvailabilityService : Sklep: ...
    //   INFO ... [o-auto-1-exec-3] [8107e1277d9c0705af5c4f03ac7b198b-e5a07db11a5ffaa3] ...StockController     : Magazyn: ...
    // Boot wstawia [traceId-spanId] z MDC (właściwość logging.pattern.correlation) — wystarczy tracing na classpath.
    // Bez aktywnego śledzenia w tym miejscu stoją puste nawiasy (widać je w logach startu aplikacji).
    //
    // DOBRA PRAKTYKA: zwracaj traceId klientowi przy błędzie (np. w ProblemDetail — s06_error_handling/Err04ProblemDetail).
    //   Dlaczego? Zgłoszenie „nie działa, kod błędu 8107e127...” prowadzi prosto do właściwych linii logu.

    // =================================================================================================
    // 6. EKSPORT — OTLP DO JAEGERA (application-ext04.properties, compose/ext04/compose.yaml)
    // =================================================================================================
    //
    //   management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
    //   spring.application.name=sklep-ext04          ← service.name w Jaegerze (bez niej: „unknown_service”)
    //
    // Dopiero adres tworzy bean OtlpHttpSpanExporter (sprawdzone uruchomieniem: bez właściwości eksportera nie ma).
    // BatchSpanProcessor wysyła odcinki paczkami co management.opentelemetry.tracing.export.schedule-delay (5 s).
    // Sprawdzone z Jaegerem 2.11: usługa sklep-ext04, w śladzie odcinki z diagramu „JAK TO DZIAŁA”.
    //
    // PUŁAPKA: właściwości z Boota 3. management.otlp.tracing.endpoint w Boot 4 jest wycofana (poziom „error”, zamiennik
    //   management.opentelemetry.tracing.export.otlp.endpoint), a management.tracing.enabled — zamiennik
    //   management.tracing.export.enabled. Dlaczego groźne? Stara nazwa jest po cichu IGNOROWANA: aplikacja startuje bez
    //   błędu, ale eksportera nie ma (sprawdzone uruchomieniem) — ślady nigdzie nie trafiają.

    // =================================================================================================
    // 7. PRÓBKOWANIE (SAMPLING)
    // =================================================================================================
    //
    //   management.tracing.sampling.probability=0.1     ← domyślnie: zapisz 10% śladów
    //   management.opentelemetry.tracing.sampler=parent-based-trace-id-ratio   ← domyślny próbnik
    // „Parent based”: decyzję podejmuje KORZEŃ śladu, a dalsze usługi ją dziedziczą z flagi traceparent — dzięki temu
    // ślad jest zapisany w całości albo wcale (nie „połowa śladu”).
    // Przy 0.0 (sprawdzone testem Ext04TracingSamplingTest): traceparent nadal idzie (flaga 02 — „nie zapisuj”),
    // identyfikatory nadal są w logach, ale do eksportera nie trafia ani jeden odcinek.
    //
    // PUŁAPKA: „podłączyłem Jaegera i widzę tylko co dziesiąte zapytanie”. Dlaczego? Domyślne 10%. Lokalnie ustaw 1.0;
    //   na produkcji dobierz tak, żeby koszt przechowywania był rozsądny.

    // =================================================================================================
    // 8. TESTY — @AutoConfigureTracing I EKSPORTER W PAMIĘCI (Boot 4)
    // =================================================================================================
    //
    // Starter spring-boot-starter-opentelemetry-test ustawia w KAŻDYM teście management.tracing.export.enabled=false
    // (i wyłącza eksport metryk). Skutek (sprawdzone testem Ext04TracingDisabledInTestsTest): odcinki lokalne i traceId
    // w logach są, ale traceparent NIE jest wysyłany — magazyn loguje INNY traceId, ślad „rozpada się” na dwa.
    // Test o śledzeniu potrzebuje:
    //   @AutoConfigureTracing                                   ← włącza eksport i propagację
    //   properties = "management.tracing.sampling.probability=1.0"
    //   @Bean InMemorySpanExporter inMemorySpanExporter()       ← Boot dołącza każdy bean SpanExporter
    //   tracerProvider.forceFlush()                             ← BatchSpanProcessor wysyła asynchronicznie
    // Odcinek serwera kończy się chwilę PO wysłaniu odpowiedzi, więc test czeka (Awaitility), aż pojawi się korzeń.
    //
    // CO CZYTAĆ DALEJ: dokumentacja Spring Boot — Observability/Tracing, Micrometer Tracing (baggage — własne pola
    // przekazywane z traceparent), OpenTelemetry (semantic conventions — standardowe nazwy atrybutów), Grafana Tempo
    // i obraz grafana/otel-lgtm (ślady + metryki + logi w jednym), Zipkin (spring-boot-starter-zipkin).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Starter: spring-boot-starter-opentelemetry; w testach: spring-boot-starter-opentelemetry-test.
     *   • Ślad = drzewo odcinków jednego zapytania; odcinek: nazwa, kind (SERVER/CLIENT/INTERNAL), rodzic, atrybuty.
     *   • traceparent: 00-traceId(32)-spanIdRodzica(16)-flagi (01 = zapisz, 02 = losowy traceId).
     *   • Klienci HTTP tylko z beanów RestClient.Builder/WebClient.Builder — inaczej ślad się urywa.
     *   • Własny odcinek: Observation.createNotStarted(...).contextualName(...).observe(...).
     *   • Logi: [traceId-spanId] automatycznie (logging.pattern.correlation).
     *   • Eksport: management.opentelemetry.tracing.export.otlp.endpoint (NIE stare management.otlp.tracing.endpoint).
     *   • Próbkowanie: management.tracing.sampling.probability (domyślnie 0.1).
     *   • Testy: @AutoConfigureTracing + InMemorySpanExporter + forceFlush(); bez adnotacji brak propagacji.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się ślad od odcinka? Jakie odcinki powstają dla GET /api/shop/ELE-001/availability?
     *   2. Rozbierz nagłówek 00-8107e1277d9c0705af5c4f03ac7b198b-57f40112a07e7c4d-03 na części. Czyj to spanId?
     *   3. Co się stanie? AvailabilityService buduje klienta przez RestClient.create() zamiast z RestClient.Builder.
     *   4. ZNAJDŹ BŁĄD: w application.properties aplikacji na Boot 4 jest
     *      management.otlp.tracing.endpoint=http://localhost:4318/v1/traces — a w Jaegerze pusto.
     *   5. Co się stanie? Test bez @AutoConfigureTracing sprawdza, że magazyn dostał nagłówek traceparent.
     *   6. Jaegera podłączono, ale widać tylko część zapytań. Dlaczego i co zmienić?
     *   7. Dlaczego numer zamówienia nie powinien być lowCardinalityKeyValue?
     *   8. Przy próbkowaniu 0.0 — czy w logach nadal jest traceId? Czy magazyn dostaje traceparent?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ext04TracingExercisesTest (zwykłe testy jednostkowe)
    // =================================================================================================

    /** Rozebrany nagłówek traceparent (ćwiczenie 1). */
    public record TraceParent(String version, String traceId, String parentSpanId, boolean sampled) {
    }

    /** Uproszczony odcinek: nazwa, własny identyfikator i identyfikator rodzica (ćwiczenie 3). */
    public record SpanInfo(String name, String spanId, String parentSpanId) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): rozbierz nagłówek traceparent na TraceParent. Format: cztery części oddzielone myślnikiem —
     * wersja (2 znaki), traceId (32), parentSpanId (16), flagi (2) — wszystkie szesnastkowe małymi literami.
     * sampled = najmłodszy bit flag ustawiony (flagi 01 i 03 → true, 00 i 02 → false). Zły format → IllegalArgumentException.
     * Podpowiedź: split("-"), matches("[0-9a-f]{32}"), Integer.parseInt(flagi, 16) {@code &} 1.
     */
    static TraceParent exercise1(String header) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj nagłówek traceparent wersji 00: "00-" + traceId + "-" + spanId + "-" + flagi, gdzie
     * flagi = "01" dla sampled, "00" w przeciwnym razie. Sprawdź dane: traceId 32 znaki szesnastkowe, spanId 16, oba
     * NIE mogą składać się z samych zer (standard W3C uznaje je za nieprawidłowe) — inaczej IllegalArgumentException.
     */
    static String exercise2(String traceId, String spanId, boolean sampled) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): narysuj drzewo odcinków jak Jaeger. Korzeń ma parentSpanId "0000000000000000".
     * Każdy poziom w głąb = 2 spacje wcięcia. Dzieci jednego rodzica — alfabetycznie po nazwie. Odcinki przychodzą
     * w DOWOLNEJ kolejności (eksporter dostaje je w kolejności kończenia — dzieci przed rodzicami).
     * Przykład: [http get (CLIENT, rodzic B), korzeń A, sprawdz-dostepnosc B (rodzic A)] →
     * ["http get /api/shop/{sku}/availability", "  sprawdz-dostepnosc", "    http get"].
     * Podpowiedź: Map rodzic → lista dzieci (groupingBy), potem rekurencja od korzenia (t16_streams/Streams11GroupingBy).
     */
    static List<String> exercise3(List<SpanInfo> spans) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Ślad = cała droga jednego zapytania (wspólny traceId); odcinek = jedna operacja w tej drodze (własny spanId,
     *      rodzic, czas, atrybuty). Dla tego zapytania (test i Jaeger): SERVER „http get /api/shop/{sku}/availability”,
     *      4 odcinki Spring Security, INTERNAL „sprawdz-dostepnosc”, CLIENT „http get”, SERVER „http get
     *      /api/stock/{sku}” i drugie 4 odcinki Security po stronie magazynu.
     *   2. 00 = wersja; 8107e127...198b = traceId; 57f40112a07e7c4d = spanId RODZICA — odcinka CLIENT „http get” w sklepie
     *      (test: stockServer.parentSpanId == client.spanId); 03 = flagi: zapisz (01) + losowy traceId (02).
     *   3. Ślad się urwie: RestClient.create() nie ma obserwacji, więc nie wyśle traceparent; magazyn zacznie nowy ślad
     *      z innym traceId, a w śladzie sklepu zniknie odcinek CLIENT. Błędu nie będzie. Sprawdzone testem
     *      plainRestClientBreaksTrace.
     *   4. W Boot 4 ta nazwa jest wycofana (poziom „error”) i po cichu ignorowana — eksporter OTLP nie powstaje
     *      (sprawdzone uruchomieniem). Poprawnie: management.opentelemetry.tracing.export.otlp.endpoint=...
     *   5. Test nie przejdzie: starter opentelemetry-test ustawia management.tracing.export.enabled=false, więc nagłówek
     *      nie jest wysyłany (receivedTraceparent = null), a magazyn ma inny traceId. Sprawdzone testem
     *      Ext04TracingDisabledInTestsTest. Rozwiązanie: @AutoConfigureTracing.
     *   6. Domyślne próbkowanie 0.1 — zapisywane jest ok. 10% śladów. Ustaw management.tracing.sampling.probability
     *      (lokalnie 1.0).
     *   7. Bo niska liczność = tag metryki: każda nowa wartość to nowa seria czasowa; tysiące numerów zamówień
     *      „wysadzą” system metryk. Numer zamówienia → highCardinalityKeyValue (tylko w odcinku).
     *   8. Tak i tak: identyfikator jest w logach, a traceparent idzie z flagą 02 (bez bitu „zapisz”); do eksportera nie
     *      trafia żaden odcinek. Sprawdzone testem Ext04TracingSamplingTest.
     */
    // </editor-fold>
}
