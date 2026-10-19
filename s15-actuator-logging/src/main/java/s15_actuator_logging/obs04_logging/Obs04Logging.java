package s15_actuator_logging.obs04_logging;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Logowanie — SLF4J i Logback, poziomy, grupy, MDC, JSON i zmiana poziomu w locie
 *        (log = dziennik zdarzeń; logger = rejestrator; level = poziom; pattern = wzorzec; appender = „ujście” logów,
 *        np. konsola albo plik; MDC = mapa kontekstu diagnostycznego; structured = strukturalne)
 *
 * W SKRÓCIE:
 *   Dobry log odpowiada na pytanie „co się właśnie stało i dlaczego?” — także o trzeciej w nocy, gdy nie masz
 *   debuggera. W Spring Boot piszesz przez SLF4J (fasada — jeden interfejs), a pod spodem działa Logback (domyślny).
 *   Decydujesz o czterech rzeczach: POZIOM (co jest ważne), WZORZEC lub FORMAT (jak wygląda linia, tekst czy JSON),
 *   MIEJSCE (konsola, plik) i KONTEKST (np. identyfikator zapytania w MDC). Poziom zmienisz bez restartu przez
 *   /actuator/loggers.
 *
 * ANALOGIA: dziennik pokładowy kapitana.
 *   Kapitan zapisuje zdarzenia z oceną ważności: „widzę mewy” (DEBUG) to szczegół, „wypłynęliśmy z portu” (INFO) to
 *   zwykły fakt, „kompas pokazuje dziwnie” (WARN) to ostrzeżenie, „woda w ładowni” (ERROR) to awaria. Na spokojnym
 *   rejsie czyta się tylko INFO i wyżej; gdy coś się dzieje, kapitan każe zapisywać więcej (zmiana poziomu w locie).
 *   Do każdego wpisu dopisuje numer rejsu (MDC), żeby potem odnaleźć wszystkie wpisy jednej wyprawy.
 *
 * JAK TO DZIAŁA:
 *   1. private static final Logger log = LoggerFactory.getLogger(Klasa.class); — nazwa rejestratora = nazwa klasy.
 *   2. Poziomy od najniższego: TRACE, DEBUG, INFO, WARN, ERROR (+ OFF). Komunikat poziomu P pojawia się, gdy P jest
 *      równy lub wyższy niż poziom rejestratora. W Boot domyślnie rejestrator główny (root) ma INFO.
 *   3. Rejestratory tworzą hierarchię po kropkach: poziom dziedziczy się z najbliższego przodka, który ma ustawiony
 *      własny: pakiet s15_actuator_logging.obs04_logging dziedziczy z s15_actuator_logging, a potem z ROOT.
 *   4. Ustawienia: logging.level.root=WARN · {@code logging.level.<pakiet>}=DEBUG ·
 *      logging.group.sklep={@code <rejestrator1>,<rejestrator2>} + logging.level.sklep=DEBUG
 *      (Boot ma gotowe grupy "web" i "sql").
 *   5. Wygląd linii: logging.pattern.console / logging.pattern.file; lub JSON: logging.structured.format.console
 *      (w Boot 4.1.1 dopuszczalne wartości to ecs, gelf i logstash).
 *   6. Plik: logging.file.name=target/aplikacja.log (albo logging.file.path=katalog). Plik nie zawiera kolorów ANSI.
 *   7. Zmiana w locie: POST {@code /actuator/loggers/<nazwa>} {"configuredLevel":"DEBUG"} (po wystawieniu punktu loggers),
 *      powrót do poziomu dziedziczonego: {"configuredLevel":null}.
 *
 *   DOMYŚLNA LINIA w Boot 4.1.1 (skopiowana z uruchomienia aplikacji lekcji Obs01Actuator):
 *     2026-10-19T11:19:51.893+02:00  INFO 4140 --- [           main] s.obs01_actuator.Obs01Application        : Started Obs01Application in 2.265 seconds (process running for 2.634)
 *     data i czas · poziom · PID · wątek · skrócona nazwa rejestratora · komunikat
 *
 *   LINIA W FORMACIE ECS (logging.structured.format.console=ecs), z testu lekcji, komunikat z MDC (requestId):
 *     {"@timestamp":"2026-10-19T12:28:36.017878100Z","log":{"level":"INFO","logger":"s15_actuator_logging.obs04_logging.CheckoutService"},
 *      "process":{"pid":35564,"thread":{"name":"main"}},"service":{"node":{}},"message":"Złożono zamówienie ZAM-001 (1 x ELE-001),
 *      klient j***@example.com","requestId":"REQ-9","ecs":{"version":"8.11"}}
 *   i w formacie logstash: pola "@version", "message", "logger_name", "thread_name", "level", "level_value", "requestId".
 *   Wniosek: MDC trafia do JSON-a automatycznie jako zwykłe pole — po to są logi strukturalne: system logów
 *   (np. Elasticsearch) może szukać po polu requestId, bez wycinania tekstu wyrażeniami regularnymi.
 *
 * SŁÓWKA:
 *   logger = rejestrator; level = poziom; trace/debug/info/warn/error = śledzenie/diagnostyka/informacja/ostrzeżenie/błąd;
 *   root = korzeń (rejestrator główny); group = grupa; appender = ujście logów; pattern = wzorzec;
 *   structured logging = logowanie strukturalne (JSON zamiast tekstu); MDC = mapa kontekstu diagnostycznego;
 *   stack trace = ślad stosu (lista wywołań prowadząca do wyjątku); placeholder = symbol zastępczy {};
 *   mask = zamaskować (ukryć fragment danych); personal data = dane osobowe; filter = filtr (sito dla zapytań).
 *
 * ZOBACZ TEŻ: s15_actuator_logging/Obs01Actuator (wystawianie punktu loggers),
 *             s00_start/Start08Debugging (czytanie logów przy błędach startu),
 *             s02_configuration/Cfg03Profiles (profile i ustawienia per środowisko),
 *             s19_deploy/Dep03ProductionConfig (logowanie na produkcji),
 *             t18_io_files/Io10SimpleLogger (własny prosty logger w czystej Javie — dla porównania).
 * </pre>
 */
public final class Obs04Logging {

    private Obs04Logging() {
    }

    // =================================================================================================
    // 1. POZIOMY — domyślnie INFO (test DefaultLevel)
    // =================================================================================================
    //
    // CheckoutService loguje DEBUG „Sprawdzam dostępność...” i INFO „Złożono zamówienie...”. Z domyślnym poziomem INFO
    // widać tylko drugi komunikat (test DefaultLevel). Dobór poziomu:
    //   ERROR — operacja się NIE udała i ktoś powinien zareagować (płatność odrzucona przez błąd systemu);
    //   WARN  — coś nietypowego, ale system radzi sobie (ponowienie, wartość domyślna);
    //   INFO  — ważne zdarzenia biznesowe i cyklu życia (złożono zamówienie, start aplikacji);
    //   DEBUG — szczegóły dla programisty (parametry, decyzje); domyślnie wyłączone;
    //   TRACE — jeszcze drobniejsze szczegóły (wejście/wyjście z metod).
    //
    // DOBRA PRAKTYKA: INFO zostaw na zdarzenia, które chcesz widzieć na produkcji przy zwykłym ruchu — a nie na każde
    //   wywołanie metody. Dlaczego? Log pełen szumu ukrywa ważne linie, kosztuje miejsce na dysku i spowalnia aplikację.

    // =================================================================================================
    // 2. logging.level.<pakiet> I GRUPY (testy LevelFromProperty, LogGroup)
    // =================================================================================================
    //
    //   logging.level.s15_actuator_logging.obs04_logging=DEBUG    ← cały pakiet (test: widać DEBUG z obu klas)
    //   logging.group.sklep=...CheckoutService,...PaymentService
    //   logging.level.sklep=DEBUG                                  ← grupa = jedna nazwa na kilka rejestratorów
    //
    // PUŁAPKA: logging.level.root=DEBUG „żeby zobaczyć, co się dzieje”. Dlaczego to zły pomysł? Włącza DEBUG dla
    //   wszystkich bibliotek (Spring, Hibernate, Tomcat) — tysiące linii na sekundę, a Twoje ginie w tłumie. Włączaj
    //   DEBUG dla jednego pakietu albo grupy, a gdy trzeba tymczasowo szerzej — zrób to w locie (sekcja 3).
    //
    // UWAGA o testach: Boot konfiguruje Logback tylko RAZ na JVM. Poziomy z logging.level.* stosują się przy każdym
    //   kontekście, ale wzorzec, format strukturalny i plik — tylko przy pierwszym. Dlatego testy tej lekcji używają
    //   pomocnika LoggingTestSupport.reset() (przywraca stan „sprzed startu aplikacji”).

    // =================================================================================================
    // 3. ZMIANA POZIOMU W LOCIE: /actuator/loggers (test RuntimeLevelChange)
    // =================================================================================================
    //
    //   GET  /actuator/loggers/s15_actuator_logging.obs04_logging
    //        → {"effectiveLevel":"INFO"}                        (nie ustawiono własnego; dziedziczy)
    //   POST /actuator/loggers/s15_actuator_logging.obs04_logging   {"configuredLevel":"DEBUG"}
    //        → 204 No Content; od następnego zapytania DEBUG jest widoczny
    //   POST ... {"configuredLevel":null}                        → powrót do dziedziczonego poziomu
    //
    // To najlepsze narzędzie do diagnozowania problemu „działa tylko na produkcji”: włączasz DEBUG dla jednego
    // pakietu, odtwarzasz problem, wyłączasz — bez restartu i bez wdrażania nowej wersji. Zmiana żyje do restartu.
    //
    // PUŁAPKA: punkt loggers wystawiony publicznie z możliwością zapisu. Dlaczego to groźne? Każdy, kto dotrze do
    //   adresu, może włączyć TRACE dla całej aplikacji i zalać dysk logami (atak typu DoS — odmowa usługi) albo
    //   wymusić zapis wrażliwych danych. Chroń go (s12_security/Sec02Authorization) albo wystaw na osobnym porcie.

    // =================================================================================================
    // 4. PARAMETRY {}, WYJĄTKI I SKLEJANIE NAPISÓW (testy w klasie głównej i ExceptionsAndPrivacy)
    // =================================================================================================
    //
    // PRZED:  log.debug("Dane: " + obiekt);          // tekst budowany ZAWSZE, nawet gdy DEBUG jest wyłączony
    // PO:     log.debug("Dane: {}", obiekt);         // tekst składany dopiero, gdy poziom jest włączony
    // Test: obiekt zliczający wywołania toString() — przy sklejaniu 1 wywołanie, przy {} zero (DEBUG wyłączony).
    //
    // PUŁAPKA: łączenie napisów plusem w logach. Dlaczego? Gdy DEBUG jest wyłączony, a obiekt ma kosztowne toString()
    //   (np. serializuje całą listę zamówień), płacisz za tekst, którego nikt nie przeczyta. Na gorącej ścieżce
    //   (wywoływanej tysiące razy na sekundę) to zauważalne obciążenie.
    //
    // WYJĄTKI — cztery warianty (test exceptionVariants):
    //   log.error("A: " + e);               → jedna linia z tekstem wyjątku, BEZ śladu stosu
    //   log.error("B: {}", e.getMessage()); → jedna linia, BEZ śladu stosu
    //   log.error("C: {}", e);              → "C: {}" (nawias zostaje dosłownie!) + ślad stosu
    //   log.error("D: komunikat", e);       → komunikat + ślad stosu  ← zalecany zapis
    // Wariant C pokazuje, że Logback traktuje OSTATNI argument typu Throwable jako wyjątek i nie wstawia go do {}.
    //
    // PUŁAPKA: gubienie śladu stosu (warianty A i B). Dlaczego? Sam komunikat „Kwota płatności musi być dodatnia”
    //   nie mówi, GDZIE w kodzie to się stało i kto wywołał metodę. Ślad stosu to mapa dojścia do błędu — bez niego
    //   szukasz po omacku. Wyjątek zawsze jako OSTATNI argument.
    //
    // PUŁAPKA: logowanie danych osobowych i sekretów (e-mail, PESEL, numer karty, hasło, token). Dlaczego? Logi trafiają
    //   do wielu miejsc (pliki, system logów, ludzie z uprawnieniami do logów), żyją długo i łatwo je skopiować — a
    //   prawo (RODO) traktuje dane osobowe w logach jak każde inne dane. CheckoutService maskuje e-mail:
    //   "jan@example.com" → "j***@example.com" (test emailIsMasked: jawnego adresu nie ma w całym wyjściu).
    //   Najlepiej w ogóle nie logować — albo logować identyfikator wewnętrzny (id klienta), nie dane.

    // =================================================================================================
    // 5. MDC — KONTEKST ZAPYTANIA (testy MdcAndPattern)
    // =================================================================================================
    //
    // RequestIdFilter wkłada do MDC identyfikator z nagłówka X-Request-Id (albo wygenerowany UUID) i USUWA go w
    // finally. Wzorzec logging.pattern.console=%5p [%X{requestId:-brak}] %m%n daje linie:
    //   " INFO [REQ-MDC] Złożono zamówienie ZAM-001 (1 x ELE-001), klient j***@example.com"
    // a logi spoza zapytania — "[brak]" (część ":-brak" to wartość domyślna). Teraz wszystkie linie jednego zapytania
    // da się odnaleźć po jednym identyfikatorze — także między klasami i bibliotekami.
    //
    // PUŁAPKA: MDC.put bez MDC.remove w finally. Dlaczego? MDC jest związane z WĄTKIEM, a serwer używa wątków z puli
    //   wielokrotnie — identyfikator poprzedniego zapytania „przykleiłby się” do logów następnego, a wątki asynchroniczne
    //   (inne wątki niż ten, który ustawił MDC) w ogóle go nie widzą (s16_cache_scheduling_async/Async04Async).

    // =================================================================================================
    // 6. LOGI STRUKTURALNE (testy StructuredEcs, StructuredLogstash) I PLIK (test LogToFile)
    // =================================================================================================
    //
    // logging.structured.format.console=ecs → każda linia to jeden obiekt JSON (pola z sekcji „Jak to działa”).
    // Test parsuje linię Jacksonem i sprawdza log.level, message i requestId. Dlaczego JSON? Systemy logów
    // (Elastic, Loki, Datadog) przeszukują pola, a nie tekst. Format ecs (Elastic Common Schema) i logstash mają różne
    // nazwy pól (log.level vs level) — wybierz ten, który rozumie Twój system.
    // logging.file.name=target/obs04-test.log → te same linie także do pliku (bez kolorów — test LogToFile).
    // Format JSON dla pliku: logging.structured.format.file.
    //
    // DOBRA PRAKTYKA: na produkcji w kontenerze loguj na konsolę (stdout) w JSON-ie, a zbieraniem plików zajmuje się
    //   platforma (Kubernetes, Docker). Dlaczego? Aplikacja nie musi znać rotacji plików ani miejsca na dysku, a
    //   system logów dostaje gotowe pola zamiast tekstu do rozbierania.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • private static final Logger log = LoggerFactory.getLogger(Klasa.class); poziomy TRACE<DEBUG<INFO<WARN<ERROR.
     *   • Domyślnie root = INFO. logging.level.root / logging.level.<pakiet> / logging.group.<nazwa>.
     *   • Parametry: log.info("Zamówienie {}", id) — NIGDY sklejanie plusem. Wyjątek jako OSTATNI argument.
     *   • Wygląd: logging.pattern.console / file; JSON: logging.structured.format.console = ecs | gelf | logstash.
     *   • Plik: logging.file.name albo logging.file.path.
     *   • MDC: MDC.put w filtrze, MDC.remove w finally; we wzorcu %X{klucz:-domyślna}; w JSON-ie — zwykłe pole.
     *   • W locie: POST /actuator/loggers/<nazwa> {"configuredLevel":"DEBUG"}; null = poziom dziedziczony.
     *   • Nie loguj danych osobowych ani sekretów; maskuj albo loguj tylko identyfikatory.
     *   • Testy logów: OutputCaptureExtension (CapturedOutput); Boot konfiguruje Logback raz na JVM — reset w testach.
     *
     * PYTANIA KONTROLNE:
     *   1. Który z komunikatów „Sprawdzam dostępność” (DEBUG) i „Złożono zamówienie” (INFO) zobaczysz przy domyślnych
     *      ustawieniach i dlaczego?
     *   2. Co się stanie? Wywołujesz log.error("C: {}", e), gdzie e to wyjątek. Czy nawias {} zostanie zastąpiony
     *      tekstem wyjątku i czy będzie ślad stosu?
     *   3. Co się stanie? POST /actuator/loggers/s15_actuator_logging.obs04_logging z {"configuredLevel":"DEBUG"} —
     *      jaki kod HTTP dostaniesz i kiedy zobaczysz DEBUG w logach?
     *   4. ZNAJDŹ BŁĄD: log.info("Klient " + klient.getEmail() + " zapłacił " + kwota) — wskaż TRZY problemy.
     *   5. ZNAJDŹ BŁĄD: filtr robi MDC.put("requestId", id); chain.doFilter(...); bez MDC.remove. Co pójdzie źle?
     *   6. Co się stanie? Logujesz log.debug("Dane: " + obiekt) przy wyłączonym DEBUG — czy wywoła się obiekt.toString()?
     *   7. Jak sprawdzić „poziom efektywny” rejestratora, który nie ma własnego ustawienia, i skąd się on bierze?
     *   8. Dlaczego warto na produkcji logować w JSON-ie (format ecs), a nie tekstem?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Obs04LoggingExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): czy komunikat danego poziomu zostanie wypisany? Kolejność: {@code TRACE < DEBUG < INFO < WARN < ERROR}. Komunikat jest wypisany, gdy jego poziom jest RÓWNY lub WYŻSZY niż poziom rejestratora.
     * Wielkość liter nie ma znaczenia; nieznany poziom → IllegalArgumentException.
     * Przykłady: ("INFO", "debug") → false; ("info", "ERROR") → true; ("DEBUG", "DEBUG") → true.
     * Podpowiedź: lista LEVELS poniżej i indexOf.
     */
    static boolean exercise1(String loggerLevel, String messageLevel) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Poziomy od najniższego do najwyższego (do ćwiczenia 1). */
    static final List<String> LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    /**
     * ĆWICZENIE 2 (średnie): zamaskuj numer karty do logów — zostaw ostatnie 4 cyfry, pozostałe cyfry zamień na '*',
     * znaki niebędące cyframi (spacje, myślniki) zostaw. null → "brak". Gdy cyfr jest 4 lub mniej — zamaskuj WSZYSTKIE.
     * Przykład: "4111 1111 1111 1234" → "**** **** **** 1234"; "12" → "**".
     * Podpowiedź: policz cyfry, potem idź po znakach i licz cyfry od początku (t04_strings/Strings03StringBuilder).
     */
    static String exercise2(String cardNumber) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): wyznacz poziom EFEKTYWNY rejestratora — jak Logback. {@code configured} to mapa
     * „nazwa rejestratora → poziom”, a klucz "ROOT" oznacza rejestrator główny. Poziom bierze się z najbliższego
     * przodka (po kropkach), który jest w mapie, łącznie z samym rejestratorem; gdy żaden — z "ROOT"; gdy i tego nie
     * ma — "INFO". Przykład: mapa {ROOT=WARN, app.shop=DEBUG, app.shop.pay=ERROR}:
     * "app.shop.pay.Charge" → "ERROR"; "app.shop.Order" → "DEBUG"; "app.Other" → "WARN"; "lib.X" → "WARN".
     * Podpowiedź: skracaj nazwę od ostatniej kropki (lastIndexOf('.')) w pętli, aż znajdziesz wpis w mapie.
     */
    static String exercise3(Map<String, String> configured, String loggerName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko INFO „Złożono zamówienie”. Rejestrator główny ma domyślnie INFO, a DEBUG jest niższy, więc jest
     *      pomijany. Sprawdzone: DefaultLevel.infoYesDebugNo.
     *   2. Nie — nawias zostaje dosłownie ("C: {}"), a wyjątek idzie do śladu stosu (Logback bierze ostatni argument
     *      typu Throwable jako wyjątek). Ślad stosu JEST. Sprawdzone: ExceptionsAndPrivacy.exceptionVariants.
     *   3. 204 No Content. DEBUG zobaczysz od NASTĘPNEGO komunikatu z tego pakietu (np. od następnego zapytania) —
     *      bez restartu. Sprawdzone: RuntimeLevelChange.changeLevelAtRuntime.
     *   4. (a) Sklejanie napisów zamiast parametrów {} — tekst budowany nawet przy wyłączonym poziomie; (b) e-mail to
     *      dana osobowa, nie wolno go logować jawnie (zamaskuj albo użyj id); (c) kwota i klient bez kontekstu: brak
     *      identyfikatora zamówienia, więc linii nie da się powiązać z innymi (brakuje id zamówienia / MDC).
     *      Poprawnie: log.info("Zapłacono zamówienie {} kwotą {} zł", orderId, kwota).
     *   5. MDC jest związane z wątkiem, a serwer używa wątków wielokrotnie — identyfikator zostaje w wątku i pojawi się
     *      w logach KOLEJNYCH zapytań (z cudzym numerem). Dlatego MDC.remove w finally. Test MdcAndPattern sprawdza,
     *      że po zapytaniu MDC jest puste.
     *   6. Tak: toString() zostanie wywołane, bo sklejanie plusem buduje tekst PRZED wywołaniem log.debug.
     *      Z log.debug("Dane: {}", obiekt) — nie. Sprawdzone: concatenationBuildsTextEvenWhenDisabled.
     *   7. GET /actuator/loggers/<nazwa> pokazuje effectiveLevel (bez configuredLevel, gdy własnego nie ma). Poziom
     *      efektywny pochodzi od najbliższego przodka w hierarchii nazw (po kropkach) z ustawionym poziomem, a na końcu
     *      z ROOT (domyślnie INFO). Sprawdzone: RuntimeLevelChange (po null: effectiveLevel INFO).
     *   8. Systemy logów przeszukują pola (level, requestId, logger), a nie tekst — JSON daje je bez wycinania
     *      wyrażeniami regularnymi; MDC trafia do JSON-a automatycznie jako pole. Sprawdzone: StructuredEcs.
     */
    // </editor-fold>
}
