package s20_modulith.mod02_module_events;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;

import java.util.List;

/**
 * <pre>
 * TEMAT: Moduły rozmawiają zdarzeniami — @ApplicationModuleListener, rejestr publikacji zdarzeń i test modułu
 *        ze Scenario
 *        (event = zdarzenie; listener = słuchacz; publication = publikacja; registry = rejestr; resubmit = ponowić;
 *         incomplete = nieukończony; completed = ukończony; scenario = scenariusz)
 *
 * W SKRÓCIE:
 *   Zamiast wołać metodę innego modułu („zamówienia każą magazynowi zarezerwować towar”), moduł ogłasza FAKT:
 *   „zamówienie złożone” (zdarzenie OrderPlaced). Kto chce, ten słucha. Moduł zamówień przestaje zależeć od
 *   magazynu — zależność się odwraca: to magazyn zna typ zdarzenia zamówień. Słuchacz z adnotacją
 *   {@code @ApplicationModuleListener} działa w innym wątku, dopiero po zatwierdzeniu transakcji publikującej
 *   i we własnej transakcji. Rejestr publikacji zdarzeń (tabela EVENT_PUBLICATION) zapisuje każde zdarzenie dla
 *   każdego słuchacza w TEJ SAMEJ transakcji co zamówienie i odhacza je po udanej obsłudze — awaria słuchacza nie
 *   gubi zdarzenia, a nieukończoną publikację da się ponowić. Moduł testujemy w izolacji:
 *   {@code @ApplicationModuleTest} + {@code Scenario}.
 *
 * ANALOGIA: zeszyt zleceń zamiast telefonu.
 *   Dział zamówień nie dzwoni do magazynu (musiałby znać numer, czekać, aż ktoś odbierze, i wiedzieć, co zrobić,
 *   gdy nikt nie odbierze). Wpisuje do zeszytu „ZAM-001 złożone” — razem z zapisem zamówienia, jednym ruchem — i
 *   wraca do pracy. Magazynier co chwilę zagląda do zeszytu, robi swoje i odhacza wpis. Gdy magazynier zachoruje,
 *   wpis czeka nieodhaczony; ktoś obsłuży go później. Gdy zamówienie zostanie anulowane, zanim trafi do systemu
 *   (rollback), wpis w zeszycie też znika.
 *
 * JAK TO DZIAŁA:
 *   1. OrderManagement.placeOrder (@Transactional): INSERT do tabeli orders, potem
 *      {@code publishEvent(new OrderPlaced(...))}.
 *   2. Spring Modulith podmienia rozsyłacz zdarzeń (PersistentApplicationEventMulticaster): dla KAŻDEGO słuchacza
 *      transakcyjnego zapisuje wiersz w EVENT_PUBLICATION (status PUBLISHED) — w bieżącej transakcji.
 *   3. Zatwierdzenie (commit): zamówienie i publikacje zapisane razem. Wycofanie (rollback): nie ma ani jednego,
 *      ani drugiego, a słuchacz w ogóle nie rusza.
 *   4. Po commit Spring woła słuchacza (@TransactionalEventListener, faza AFTER_COMMIT). @Async przenosi wywołanie
 *      do puli wątków Boota (wątki task-1, task-2, …) — @EnableAsync włącza za nas autokonfiguracja Spring Modulith.
 *      @Transactional(REQUIRES_NEW) otwiera nową transakcję słuchacza.
 *   5. Sukces → publikacja COMPLETED (z datą COMPLETION_DATE). Wyjątek → FAILED, COMPLETION_DATE pusta, w logu
 *      INFO od Spring Modulith i ERROR z obsługi wyjątków metod asynchronicznych.
 *   6. Ponowienie: {@code IncompleteEventPublications} / {@code FailedEventPublications} z kodu albo
 *      {@code spring.modulith.events.republish-outstanding-events-on-restart=true} przy starcie aplikacji.
 *
 * SŁÓWKA:
 *   event = zdarzenie; publish = opublikować; listener = słuchacz; publication = publikacja (jedno zdarzenie dla
 *   jednego słuchacza); registry = rejestr; incomplete = nieukończony; completed = ukończony; failed = nieudany;
 *   resubmit = ponowić (wysłać jeszcze raz); serialized = zapisany jako tekst (tu JSON); after commit = po
 *   zatwierdzeniu; requires new = wymaga nowej (transakcji); idempotent = odporny na powtórzenie (drugie wykonanie
 *   nic nie psuje); scenario = scenariusz; standalone = samodzielny; outbox = skrzynka nadawcza.
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns06Observer (wzorzec Obserwator — źródło idei zdarzeń),
 *             s17_events/Evt01ApplicationEvents (zdarzenia w Springu), s17_events/Evt02TransactionalEvents
 *             (@TransactionalEventListener), s17_events/Evt03AsyncAndOutbox (asynchroniczność i wzorzec outbox — rejestr
 *             Spring Modulith to gotowy outbox), s10_transactions/Tx02Propagation (REQUIRES_NEW),
 *             s16_cache_scheduling_async/Async04Async (@Async i pula wątków), s20_modulith/Mod01Modules (moduły
 *             i cykle), s20_modulith/Mod03Documentation (zdarzenia na diagramach i kanwach).
 * </pre>
 */
public final class Mod02ModuleEvents {

    private Mod02ModuleEvents() {
    }

    // =================================================================================================
    // 1. ZDARZENIE ZAMIAST WYWOŁANIA — ODWRÓCENIE ZALEŻNOŚCI
    // =================================================================================================
    //
    //   PRZED (wywołanie):  orders.OrderManagement ──woła──▶ inventory.reserve(...)     orders → inventory
    //   PO (zdarzenie):     orders publikuje OrderPlaced ◀──słucha── inventory           inventory → orders
    //
    //   // orders: nie wie, kto słucha
    //   @Transactional
    //   public void placeOrder(String orderId, String sku, int quantity) {
    //       jdbc.sql("INSERT INTO orders ...").update();
    //       events.publishEvent(new OrderPlaced(orderId, sku, quantity));
    //   }
    //
    //   // inventory (pakiet internal): reaguje sam
    //   @ApplicationModuleListener
    //   public void on(OrderPlaced event) { ... UPDATE stock ...; events.publishEvent(new StockReserved(...)); }
    //
    // Opis modułów (System.out.println(ApplicationModules.of(Mod02Application.class))) — prawdziwy wynik, fragment:
    //
    //   # Magazyn
    //   > Logical name: inventory
    //   > Direct module dependencies:
    //     - orders
    //   > Spring beans:
    //     + ….InventoryManagement
    //     o ….internal.InventoryEventHandler
    //
    // „- orders” (minus, nie plus): magazyn zależy od zamówień tylko przez TYP zdarzenia, nie przez bean — do testu
    // modułu magazynu (sekcja 6) nie trzeba startować beanów modułu zamówień. Dodanie kolejnego odbiorcy (np.
    // wysyłki) nie zmienia ani linijki w orders. Test sekcji 1 sprawdza kierunek zależności
    // i getEventsListenedTo() (zdarzenia, których słucha moduł: [OrderPlaced]).
    //
    // DOBRA PRAKTYKA: zdarzenie = mały, niezmienny rekord w API modułu publikującego, nazwany w czasie przeszłym
    //   (OrderPlaced, StockReserved), z identyfikatorami i danymi potrzebnymi słuchaczom. Dlaczego? Rejestr zapisuje
    //   je jako JSON (kolumna VARCHAR(4000)) i odtwarza później — może w innym wątku albo po restarcie. Encja JPA czy
    //   obiekt zmienny w zdarzeniu = kłopoty z serializacją i dane, które zdążyły się zmienić.
    //
    // PUŁAPKA: zdarzenia nie są magicznym lekarstwem na cykle. Gdyby orders słuchało StockReserved (typ z inventory),
    //   a inventory słuchało OrderPlaced (typ z orders), oba moduły znałyby typy drugiego — verify() zgłasza wtedy
    //   „Cycle detected: Slice inventory -> …” (sprawdzone uruchomieniem). Dlaczego? Dla analizy modułów typ
    //   zdarzenia to zwykła zależność od typu. Zdarzenia rozcinają cykl tylko wtedy, gdy jeden kierunek zostaje
    //   zastąpiony nasłuchiwaniem, a nie dołożony.

    // =================================================================================================
    // 2. @ApplicationModuleListener — ASYNCHRONICZNIE, PO COMMIT, WE WŁASNEJ TRANSAKCJI
    // =================================================================================================
    //
    //   @ApplicationModuleListener   ≡   @Async
    //                                    @Transactional(propagation = Propagation.REQUIRES_NEW)
    //                                    @TransactionalEventListener            // domyślnie faza AFTER_COMMIT
    //
    // Test sekcji 2 używa „sondy” — drugiego słuchacza, który zapisuje swój wątek i transakcję. Prawdziwy wynik:
    //   wątek: np. task-2 (pula wątków Boota, nie wątek testu „main”), transakcja aktywna: true,
    //   nazwa transakcji: s20_modulith.mod02_module_events.Mod02ModuleEventsTest$ListenerProbe.on
    // Gdy transakcja publikująca zostanie wycofana: brak zamówienia, brak wierszy w EVENT_PUBLICATION, słuchacz
    // nie dostaje zdarzenia (też sprawdzone testem).
    //
    // Po co każda z trzech części?
    //   • AFTER_COMMIT — słuchacz nie zareaguje na zamówienie, którego ostatecznie nie ma (rollback).
    //   • @Async — wolny lub zepsuty słuchacz nie spowalnia i nie psuje składania zamówienia.
    //   • REQUIRES_NEW — po AFTER_COMMIT transakcji publikującego już nie ma; słuchacz zapisuje do bazy we własnej.
    // Porównaj ze zwykłym @EventListener: wywołanie synchroniczne, w tej samej transakcji — wyjątek słuchacza
    // wycofuje także zamówienie (lekcja Evt02TransactionalEvents).
    //
    // PUŁAPKA: test, który zaraz po placeOrder(...) sprawdza stan magazynu. Słuchacz biegnie w innym wątku, więc
    //   asercja wygrywa albo przegrywa wyścig z nim (w naszym uruchomieniu stan był jeszcze stary). Dlaczego to zły
    //   test? Raz przechodzi, raz nie (flaky). Czekaj na skutek: Awaitility {@code await().until(...)} albo Scenario.
    //   Nigdy Thread.sleep — za krótko = losowe błędy, za długo = wolne testy.
    //
    // PUŁAPKA: bean ze słuchaczem @ApplicationModuleListener jest PROXY (AopUtils.isAopProxy == true — sprawdzone
    //   testem), bo @Async i @Transactional dokłada AOP. Pola czytane bezpośrednio z obiektu proxy (np. przez pole
    //   pakietowe w teście) mają wartość null — proxy CGLIB to podklasa z własnymi, nieustawionymi polami.
    //   Dlaczego działa wołanie metod? Proxy przekazuje je do prawdziwego obiektu. Stan słuchacza udostępniaj
    //   metodami (jak sonda w teście: runFor(orderId)).

    // =================================================================================================
    // 3. REJESTR PUBLIKACJI ZDARZEŃ (EVENT_PUBLICATION)
    // =================================================================================================
    //
    // Starter spring-modulith-starter-jdbc dokłada rejestr na JDBC. Jedno zdarzenie z dwoma słuchaczami = DWA
    // wiersze (publikacje). Prawdziwy wiersz po udanej obsłudze (test sekcji 3):
    //
    //   EVENT_TYPE          s20_modulith.mod02_module_events.orders.OrderPlaced
    //   LISTENER_ID         s20_modulith.mod02_module_events.inventory.internal.InventoryEventHandler
    //                         .on(s20_modulith.mod02_module_events.orders.OrderPlaced)
    //   SERIALIZED_EVENT    {"orderId":"ZAM-301","sku":"ELE-003","quantity":1}
    //   STATUS              COMPLETED          (cykl: PUBLISHED → PROCESSING → COMPLETED albo FAILED → RESUBMITTED …)
    //   COMPLETION_DATE     data ukończenia (pusta = publikacja nieukończona)
    //   COMPLETION_ATTEMPTS 1                  (liczba podejść; ponowienie zwiększa ją o 1)
    //
    // To jest wzorzec OUTBOX (skrzynka nadawcza) w gotowej postaci: zapis biznesowy i „list do wysłania” idą w jednej
    // transakcji, więc nie da się zgubić zdarzenia ani wysłać zdarzenia o czymś, czego nie zapisano
    // (porównaj lekcję Evt03AsyncAndOutbox, gdzie outbox budujemy ręcznie).
    //
    // Co z ukończonymi publikacjami? Domyślnie (spring.modulith.events.completion-mode=update) zostają w tabeli
    // z datą ukończenia. Inne tryby: delete (usuwane od razu) i archive (przenoszone do EVENT_PUBLICATION_ARCHIVE).
    // Sprzątanie z kodu: CompletedEventPublications.deletePublicationsOlderThan(Duration).
    //
    // DOBRA PRAKTYKA: przy trybie update zaplanuj sprzątanie ukończonych publikacji (np. starszych niż 7 dni).
    //   Dlaczego? Każde zdarzenie × każdy słuchacz = nowy wiersz; po miesiącach tabela puchnie i spowalnia wyszukiwanie
    //   nieukończonych.

    // =================================================================================================
    // 4. AWARIA SŁUCHACZA I PONAWIANIE
    // =================================================================================================
    //
    // Zamówienie ZAM-401 na produkt NOW-001, którego magazyn nie zna: słuchacz rzuca IllegalStateException.
    // Prawdziwy log (skrócony):
    //   INFO  … Invocation of listener public void …InventoryEventHandler.on(…OrderPlaced) failed with message
    //         Nie można zarezerwować 1 szt. NOW-001 dla zamówienia ZAM-401. Leaving event publication uncompleted.
    //   ERROR … SimpleAsyncUncaughtExceptionHandler : Unexpected exception occurred invoking async method: …
    // Skutek: zamówienie ZOSTAJE (inna transakcja niż słuchacza), publikacja magazynu: STATUS = FAILED,
    // COMPLETION_DATE = null. Po „naprawie” (magazyn poznaje NOW-001) ponawiamy:
    //
    //   incompletePublications.resubmitIncompletePublications(
    //           publication -> publication.getEvent() instanceof OrderPlaced e && e.orderId().equals("ZAM-401"));
    //
    // Ponowienie oznacza publikację jako RESUBMITTED (COMPLETION_ATTEMPTS + 1) i znów woła słuchacza — też
    // asynchronicznie. Po sukcesie: COMPLETED, COMPLETION_ATTEMPTS = 2. Test sekcji 4.
    // Inne sposoby: resubmitIncompletePublicationsOlderThan(Duration), FailedEventPublications.resubmit(
    // ResubmissionOptions.defaults()) — tylko FAILED, oraz ustawienie
    // spring.modulith.events.republish-outstanding-events-on-restart=true (ponów wszystko nieukończone przy starcie).
    //
    // PUŁAPKA: ponawianie „wszystkich nieukończonych” w działającej aplikacji. Nieukończona to także publikacja,
    //   którą słuchacz WŁAŚNIE przetwarza — ponowienie uruchomi ją drugi raz. Dlaczego to groźne? Towar
    //   zarezerwowany dwa razy. Ponawiaj FAILED albo publikacje starsze niż rozsądny czas obsługi.
    //
    // DOBRA PRAKTYKA: pisz słuchaczy IDEMPOTENTNYCH (drugie wykonanie dla tego samego zdarzenia nic nie psuje), np.
    //   zapisuj numer obsłużonego zamówienia i pomijaj duplikaty. Dlaczego? Rejestr gwarantuje dostarczenie „co
    //   najmniej raz” (at-least-once): oznaczenie publikacji jako ukończonej następuje PO zatwierdzeniu transakcji
    //   słuchacza, osobno — awaria dokładnie między tymi krokami skończy się ponowieniem już wykonanej pracy.

    // =================================================================================================
    // 5. SKĄD TABELA EVENT_PUBLICATION?
    // =================================================================================================
    //
    // Dwie drogi:
    //   • Spring Modulith 2.1 SAM zakłada tabelę przy starcie (skrypt schema-h2.sql z jar-a
    //     spring-modulith-events-jdbc, CREATE TABLE IF NOT EXISTS). Tak jest DOMYŚLNIE — sprawdza to test 5b bez
    //     profilu lekcji. Uwaga: opis właściwości w IDE (metadane konfiguracji) podaje wartość domyślną false, ale
    //     kod autokonfiguracji 2.1.1 włącza zakładanie tabeli, gdy właściwość nie jest ustawiona (sprawdzone
    //     uruchomieniem). Wierz testom, nie podpowiedziom.
    //   • Migracja Flyway z tą samą tabelą (V2__event_publication.sql — skopiowana z jar-a) i wyłączenie
    //     samodzielnego zakładania: spring.modulith.events.jdbc.schema-initialization.enabled=false (profil mod02).
    //
    // DOBRA PRAKTYKA: w projekcie z Flyway rób to drugie. Dlaczego? Schematem bazy rządzi jedno narzędzie, wersja
    //   tabeli jest zapisana w historii migracji i identyczna na każdym środowisku, a aktualizacja Spring Modulith
    //   ze zmianą schematu (jak v1 → v2: doszły kolumny STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE)
    //   wymaga świadomej migracji, a nie cichej zmiany przy starcie.
    //
    // PUŁAPKA: wyłączasz zakładanie tabeli, a migracji z EVENT_PUBLICATION nie ma. Pierwsza publikacja zdarzenia
    //   kończy się BadSqlGrammarException: „PreparedStatementCallback; bad SQL grammar [INSERT INTO EVENT_PUBLICATION
    //   (ID, EVENT_TYPE, …) VALUES (…)]” (przyczyna z H2 mówi, że tabela nie istnieje — po polsku albo po angielsku,
    //   zależnie od języka systemu). Dlaczego znika też zamówienie? Zapis publikacji jest częścią transakcji
    //   placeOrder — jego błąd wycofuje całość. Test 5a.

    // =================================================================================================
    // 6. TEST MODUŁU: @ApplicationModuleTest I Scenario
    // =================================================================================================
    //
    //   package s20_modulith.mod02_module_events.inventory;          // test w PAKIECIE MODUŁU (src/test/java)
    //
    //   @ApplicationModuleTest                                        // domyślnie BootstrapMode.STANDALONE
    //   @ActiveProfiles("mod02")
    //   class Mod02InventoryModuleTest {
    //       @Test
    //       void reservesStockAndPublishesEvent(Scenario scenario) {
    //           scenario.publish(new OrderPlaced("ZAM-601", "KSI-001", 2))       // bodziec: zdarzenie w transakcji
    //                   .andWaitForEventOfType(StockReserved.class)                 // czekaj na zdarzenie wynikowe
    //                   .matching(event -> event.orderId().equals("ZAM-601"))
    //                   .toArriveAndVerify(event -> assertThat(event.quantity()).isEqualTo(2));
    //       }
    //   }
    //
    // Co robi @ApplicationModuleTest (log: „Bootstrapping @org.springframework.modulith.test.ApplicationModuleTest
    // for Magazyn in mode STANDALONE (class s20_modulith.mod02_module_events.Mod02Application)…”):
    //   • szuka @SpringBootApplication w górę pakietów i wyznacza moduł z pakietu klasy testu;
    //   • przed startem woła verify() na wszystkich modułach (atrybut verifyAutomatically = true) — naruszenie
    //     granic przerywa test;
    //   • skanuje komponenty i autokonfigurację tylko z pakietu modułu (beanów orders nie ma — test 6c);
    //   • daje parametry Scenario i PublishedEvents oraz wstrzykiwanie przez konstruktor testu.
    // Tryby: STANDALONE (sam moduł), DIRECT_DEPENDENCIES (+ moduły, których beanów używa), ALL_DEPENDENCIES
    // (+ wszystkie w głąb). Zależność przez sam typ zdarzenia NIE wciąga modułu: z DIRECT_DEPENDENCIES beanów orders
    // nadal nie ma; dołącza je dopiero extraIncludes = "orders" (sprawdzone uruchomieniem).
    // Scenario: publish(...) albo stimulate(...) (wywołanie metody w transakcji), potem andWaitForEventOfType(...)
    // albo andWaitForStateChange(() -> stan, warunek). Czeka przez Awaitility (domyślnie do 10 s; zmiana:
    // andWaitAtMost(Duration)).
    //
    // DOBRA PRAKTYKA: każdy moduł ma test(y) @ApplicationModuleTest opisujące jego zachowanie zdarzeniami:
    //   „gdy przyjdzie X, to stanie się Y / pojawi się zdarzenie Z”. Dlaczego? Taki test startuje szybciej niż cała
    //   aplikacja, nie zależy od innych modułów i jest gotową specyfikacją kontraktu modułu.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Moduł publikuje fakt (rekord w czasie przeszłym, w swoim API); inne moduły słuchają — zależność odwrócona.
     *   • @ApplicationModuleListener = @Async + @Transactional(REQUIRES_NEW) + @TransactionalEventListener
     *     (AFTER_COMMIT); @EnableAsync włącza autokonfiguracja Spring Modulith; wątki task-N.
     *   • Rollback publikującego → zero publikacji, słuchacz nie rusza. Wyjątek słuchacza → zamówienie zostaje,
     *     publikacja FAILED.
     *   • EVENT_PUBLICATION: wiersz na zdarzenie × słuchacza; SERIALIZED_EVENT = JSON; STATUS, COMPLETION_DATE,
     *     COMPLETION_ATTEMPTS. completion-mode: update (domyślnie) / delete / archive.
     *   • Ponawianie: IncompleteEventPublications.resubmitIncompletePublications(filtr) / …OlderThan(Duration),
     *     FailedEventPublications.resubmit(ResubmissionOptions), republish-outstanding-events-on-restart=true.
     *   • Dostarczenie co najmniej raz → słuchacz idempotentny.
     *   • Tabela: domyślnie zakłada ją Modulith 2.1; z Flyway → migracja + schema-initialization.enabled=false.
     *   • Test modułu: @ApplicationModuleTest w pakiecie modułu + Scenario (publish/stimulate → andWaitFor…);
     *     asynchroniczność testuj przez czekanie (Awaitility), nigdy przez sleep.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego po wprowadzeniu zdarzenia OrderPlaced moduł orders nie zależy od inventory? W którą stronę biegnie
     *      teraz zależność i co pokazuje „- orders” w opisie modułu magazynu?
     *   2. Co się stanie? Test woła orders.placeOrder("ZAM-1", "KSI-001", 2) i w następnej linii sprawdza
     *      assertThat(inventory.stockOf("KSI-001")).contains(13) (stan przed: 15).
     *   3. Co się stanie? placeOrder(...) wykonuje się w transakcji, która na końcu zostaje wycofana (rollback).
     *      Co będzie w tabelach orders i EVENT_PUBLICATION i czy słuchacz magazynu coś zrobi?
     *   4. Co się stanie? Słuchacz magazynu rzuca wyjątek, bo nie zna produktu. Co z zamówieniem, a co z publikacją?
     *   5. ZNAJDŹ BŁĄD: ustawiasz spring.modulith.events.jdbc.schema-initialization.enabled=false, a migracje Flyway
     *      nie tworzą tabeli EVENT_PUBLICATION. Aplikacja startuje. Co się stanie przy pierwszym zamówieniu?
     *   6. Dlaczego słuchacz powinien być idempotentny, skoro rejestr „pilnuje” zdarzeń?
     *   7. ZNAJDŹ BŁĄD: test z @ApplicationModuleTest leży w pakiecie modułu inventory i sprawdza, że po
     *      orders.placeOrder(...) stan magazynu spada — wstrzykuje do tego OrderManagement. Czemu to nie zadziała
     *      i jak to naprawić?
     *   8. Czym różni się @ApplicationModuleListener od zwykłego @EventListener?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Mod02ModuleEventsExercisesTest (cała aplikacja Mod02Application, własna baza H2)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć liczbę NIEUKOŃCZONYCH publikacji w rejestrze (wiersze tabeli EVENT_PUBLICATION
     * bez daty ukończenia). Podpowiedź: {@code SELECT COUNT(*) ... WHERE COMPLETION_DATE IS NULL},
     * {@code query(Integer.class).single()}.
     */
    static int exercise1(JdbcClient jdbc) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowane PROSTE nazwy typów zdarzeń, których słucha moduł o podanej nazwie
     * (dla "inventory": [OrderPlaced], dla "orders": []). Brak modułu → {@code IllegalArgumentException}
     * „Nie ma modułu: nazwa”. Podpowiedź: {@code getModuleByName}, {@code getEventsListenedTo(modules)},
     * {@code JavaClass.getSimpleName()}.
     */
    static List<String> exercise2(ApplicationModules modules, String moduleName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): ponów nieukończone publikacje TYLKO zdarzeń OrderPlaced o podanym numerze zamówienia
     * (innych nie ruszaj). Podpowiedź: {@code resubmitIncompletePublications(filtr)}, a w filtrze
     * {@code publication.getEvent() instanceof OrderPlaced event && ...}.
     */
    static void exercise3(IncompleteEventPublications incomplete, String orderId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): posprzątaj rejestr — usuń WSZYSTKIE ukończone publikacje i zwróć, ile ich było.
     * Podpowiedź: {@code findAll().size()} przed usunięciem, potem {@code deletePublications(publication -> true)}.
     */
    static int exercise4(CompletedEventPublications completed) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo orders tylko publikuje zdarzenie i nie zna żadnego typu z inventory. Zależność biegnie odwrotnie:
     *      inventory → orders, i to wyłącznie przez typ OrderPlaced. „- orders” znaczy: zależność bez użycia beanów
     *      — test modułu magazynu nie musi startować modułu zamówień. Sprawdzone testem sekcji 1.
     *   2. Wynik zależy od wyścigu wątków: słuchacz działa asynchronicznie w wątku task-N, więc stan jest zwykle
     *      jeszcze 15 i asercja pada (w naszym uruchomieniu odczyt zaraz po placeOrder dał 15). Test niestabilny
     *      (flaky). Poprawnie: await().until(() -> inventory.stockOf("KSI-001").orElseThrow() == 13) albo Scenario.
     *   3. Pusto: zamówienia nie ma, wierszy publikacji też (były zapisane w tej samej transakcji), a słuchacz nie
     *      rusza, bo zdarzenie AFTER_COMMIT nie zostaje dostarczone przy rollbacku. Sprawdzone testem sekcji 2.
     *   4. Zamówienie zostaje (słuchacz ma własną transakcję, uruchomioną po commit zamówienia). Publikacja magazynu:
     *      STATUS FAILED, COMPLETION_DATE null, w logu INFO „… Leaving event publication uncompleted.” i ERROR
     *      z SimpleAsyncUncaughtExceptionHandler. Można ją ponowić. Sprawdzone testem sekcji 4.
     *   5. Zamówienie się nie zapisze: placeOrder rzuca BadSqlGrammarException „… bad SQL grammar [INSERT INTO
     *      EVENT_PUBLICATION …]”, a cała transakcja (z INSERT do orders) zostaje wycofana. Sprawdzone testem 5a.
     *      Naprawa: migracja z tabelą (kopia schematu z jar-a) albo włączenie schema-initialization.
     *   6. Rejestr gwarantuje dostarczenie co najmniej raz: publikacja jest oznaczana jako ukończona dopiero po
     *      zatwierdzeniu transakcji słuchacza, osobnym zapisem. Awaria między tymi krokami albo ponowienie publikacji,
     *      która właśnie się przetwarza, wywoła słuchacza drugi raz dla tego samego zdarzenia.
     *   7. Test modułu w trybie STANDALONE startuje tylko beany modułu inventory — beana OrderManagement nie ma
     *      (sprawdzone testem 6c), więc wstrzyknięcie się nie powiedzie. Zależność magazynu od zamówień to tylko typ
     *      zdarzenia, więc nawet DIRECT_DEPENDENCIES go nie dołączy (sprawdzone uruchomieniem). Naprawa: testuj
     *      kontrakt modułu — Scenario.publish(new OrderPlaced(...)) i czekaj na zmianę stanu albo StockReserved;
     *      ewentualnie @ApplicationModuleTest(extraIncludes = "orders").
     *   8. @EventListener: wywołanie synchroniczne, w wątku i w transakcji publikującego, od razu przy publishEvent —
     *      wyjątek słuchacza wycofuje też zamówienie. @ApplicationModuleListener: osobny wątek, dopiero po commit,
     *      własna transakcja, a z rejestrem — trwały zapis publikacji i możliwość ponowienia.
     */
    // </editor-fold>
}
