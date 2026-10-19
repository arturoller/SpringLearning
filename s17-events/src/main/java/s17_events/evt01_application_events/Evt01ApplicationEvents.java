package s17_events.evt01_application_events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * TEMAT: Zdarzenia aplikacji: publikowanie i @EventListener
 *        (event = zdarzenie; listener = słuchacz; publisher = publikujący, nadawca; synchronous = synchroniczny,
 *         „w tym samym wątku, na bieżąco”; payload = ładunek, czyli zawartość zdarzenia)
 *
 * W SKRÓCIE:
 *   Zamiast wywoływać po kolei magazyn, punkty lojalnościowe i powiadomienia, serwis zamówień ogłasza fakt:
 *   „zamówienie ZAM-001 zostało złożone”. Kto chce, ten nasłuchuje. Serwis nie zna słuchaczy (luźne powiązanie), a nowego
 *   słuchacza dopisujesz bez dotykania serwisu. Zdarzeniem może być zwykły rekord. Ważne: domyślnie słuchacze
 *   działają SYNCHRONICZNIE — w wątku publikującego, jeden po drugim — więc to wciąż zwykłe wywołania metod, tylko
 *   „odwrócone” (to nie jest kolejka wiadomości!).
 *
 * ANALOGIA: tablica ogłoszeń w biurze.
 *   Księgowa wiesza kartkę „Zamówienie ZAM-001 złożone”. Magazynier, dział lojalnościowy i obsługa klienta patrzą na tablicę
 *   i każdy robi swoje. Księgowa nie chodzi do każdego z osobna i nie wie, kto w ogóle czyta tablicę. Ale uwaga:
 *   w naszej wersji księgowa NIE odchodzi od tablicy, dopóki wszyscy nie skończą (synchronicznie), a jeśli ktoś
 *   z hukiem upuści kubek (wyjątek), księgowa to usłyszy.
 *
 * JAK TO DZIAŁA:
 *   1. Zdarzenie to rekord, np. {@code record OrderPlaced(String orderId, String customer, BigDecimal totalPln)}.
 *      Od Springa 4.2 nie musi dziedziczyć po {@code ApplicationEvent}; Spring opakowuje je w
 *      {@code PayloadApplicationEvent} (sprawdza to test sekcji 1).
 *   2. Publikujący wstrzykuje {@code ApplicationEventPublisher} (to sam kontekst Springa) i woła {@code publishEvent(...)}.
 *   3. Przy starcie Spring znajduje metody z {@code @EventListener} w beanach i zapisuje, na jaki TYP zdarzenia reagują
 *      (typ pierwszego parametru). Metoda w klasie, która nie jest beanem, jest ignorowana.
 *   4. publishEvent wywołuje pasujących słuchaczy jednego po drugim, w tym samym wątku, i wraca dopiero
 *      po ostatnim. Kolejność: {@code @Order} (mniejsza liczba = wcześniej).
 *   5. Warunek {@code condition} (SpEL = Spring Expression Language, język wyrażeń Springa) pozwala pominąć słuchacza.
 *   6. Słuchacz może zwrócić nowe zdarzenie — Spring opublikuje je zaraz po zakończeniu tej metody.
 *
 * SŁÓWKA:
 *   publish = opublikować; listener = słuchacz; condition = warunek; order = kolejność; payload = ładunek; loose coupling =
 *   luźne powiązanie; synchronous = synchroniczny (po kolei, w jednym wątku); thread = wątek; caller = wywołujący.
 *
 * ZOBACZ TEŻ: t22_design_patterns/Patterns06Observer (wzorzec obserwator w czystej Javie — to jest właśnie on, tylko
 *             z kontenerem w roli „listy obserwatorów”),
 *             s01_core_ioc/Ioc03Stereotypes (@Component/@Service — słuchacze muszą być beanami),
 *             s17_events/Evt02TransactionalEvents (zdarzenia powiązane z transakcją),
 *             s17_events/Evt03AsyncAndOutbox (zdarzenia w osobnym wątku i wzorzec outbox).
 * </pre>
 */
public final class Evt01ApplicationEvents {

    private Evt01ApplicationEvents() {
    }

    // =================================================================================================
    // 1. PUBLIKOWANIE ZWYKŁEGO REKORDU I @EventListener
    // =================================================================================================
    //
    //   // zdarzenie
    //   public record OrderPlaced(String orderId, String customer, BigDecimal totalPln) { }
    //
    //   // nadawca
    //   publisher.publishEvent(new OrderPlaced("ZAM-001", "Jan Kowalski", new BigDecimal("349.90")));
    //
    //   // słuchacz (bean!)
    //   @EventListener
    //   public void reserveStock(OrderPlaced event) { ... }
    //
    // Spring dopasowuje słuchacza po TYPIE parametru (także po nadtypie lub interfejsie zdarzenia). Zdarzenie nie
    // musi dziedziczyć po ApplicationEvent — w środku Spring i tak opakowuje je w PayloadApplicationEvent
    // (test sekcji 1 widzi opakowane zdarzenie, a rekord jako jego zawartość). Własne zdarzenia to zwykłe rekordy,
    // ale Spring publikuje też swoje: ContextRefreshedEvent (kontekst gotowy), ContextClosedEvent (zamknięcie),
    // ApplicationReadyEvent (Boot: aplikacja wystartowała) — nasłuchujesz ich tak samo.
    //
    // DOBRA PRAKTYKA: nazywaj zdarzenia w czasie przeszłym (OrderPlaced, nie PlaceOrder) i rób je niezmiennymi
    //   (rekord). Dlaczego? Zdarzenie opisuje fakt, który już zaszedł — słuchacz nie może go „odwołać”. Nazwa
    //   w trybie rozkazującym brzmi jak polecenie i zachęca do traktowania słuchacza jak wykonawcy zlecenia.
    //   Niezmienność chroni przed tym, że jeden słuchacz zmieni dane, które potem zobaczy drugi.

    // =================================================================================================
    // 2. DOMYŚLNIE SYNCHRONICZNIE: TEN SAM WĄTEK, PRZED ZAKOŃCZENIEM publishEvent
    // =================================================================================================
    //
    // Wpisy dziennika z uruchomienia Evt01Application (słuchacze działają w wątku publikującego, czyli main):
    //   zamówienie-przyjęte:ZAM-001
    //   magazyn:ZAM-001  →  punkty:ZAM-001  →  punkty-zapisane  →  powiadomienie:ZAM-001
    //   publikacja-zakończona:ZAM-001      ← linia PO publishEvent dopiero po wszystkich słuchaczach
    //
    // Test sekcji 2 sprawdza dwie rzeczy: ostatni wpis to „publikacja-zakończona” oraz WSZYSTKIE wpisy mają nazwę
    // wątku testu. Skutki synchroniczności: (a) wolny słuchacz spowalnia publikującego, (b) słuchacz działa w tej
    // samej transakcji i w tym samym wątku co publikujący (lekcja Evt02TransactionalEvents), (c) wyjątek słuchacza
    // wraca do publikującego (sekcja 3).
    //
    // PUŁAPKA: myślenie „zdarzenia są asynchroniczne, więc wolny słuchacz mi nie przeszkadza”.
    //   Dlaczego? Domyślny rozgłaszacz (multicaster) Springa wywołuje słuchaczy w wątku publikującego. Wolne
    //   powiadomienie e-mail w słuchaczu wydłuża odpowiedź na żądanie HTTP. Asynchroniczność trzeba włączyć
    //   osobno (@Async — lekcja Evt03AsyncAndOutbox).

    // =================================================================================================
    // 3. WYJĄTEK SŁUCHACZA WRACA DO PUBLIKUJĄCEGO
    // =================================================================================================
    //
    // Test sekcji 3: pierwszy słuchacz BrokenEvent rzuca IllegalStateException("Słuchacz zepsuty: test").
    // Wyjątek wychodzi z publishEvent do serwisu; drugi słuchacz (@Order(2)) NIE zostaje wywołany, a kod po
    // publishEvent w serwisie też się nie wykona (dziennik pozostaje pusty). Gdyby serwis był transakcyjny,
    // wyjątek niezłapany wycofałby transakcję (lekcja s10_transactions/Tx01Transactional).
    //
    // PUŁAPKA: „skoro słuchacz jest osobną klasą, jego błąd mnie nie dotyczy”.
    //   Dlaczego? Wywołanie jest zwykłą metodą w tym samym wątku. Błąd w naliczaniu punktów może wywrócić całe
    //   złożenie zamówienia, a kolejni słuchacze nie dostaną zdarzenia (ich kolejność decyduje, kogo błąd ominie).
    //
    // DOBRA PRAKTYKA: słuchacz pomocniczy (e-mail, statystyki) sam łapie i loguje wyjątki, albo dostaje zdarzenie
    //   po zatwierdzeniu transakcji i/lub asynchronicznie (kolejne lekcje). Dlaczego? Awaria dodatku nie powinna
    //   odwoływać głównej operacji, o ile tak postanowiłeś; jeśli ma ją odwoływać — zostaw wyjątek świadomie.

    // =================================================================================================
    // 4. KOLEJNOŚĆ: @Order
    // =================================================================================================
    //
    //   @Order(1) @EventListener void reserveStock(OrderPlaced e)
    //   @Order(2) @EventListener PointsGranted grantPoints(OrderPlaced e)
    //   @Order(3) @EventListener void notifyCustomer(OrderPlaced e)
    //
    // Mniejsza liczba = wcześniej. Bez @Order kolejność jest nieokreślona — nie polegaj na niej. Test sekcji 4
    // sprawdza: magazyn → punkty → powiadomienie. Jeśli kolejność słuchaczy jest ważna dla poprawności („najpierw
    // magazyn, potem płatność”), to znak, że zdarzenie ukrywa przepływ, który lepiej zapisać jawnie (sekcja 8).

    // =================================================================================================
    // 5. WARUNEK condition (SpEL)
    // =================================================================================================
    //
    //   @EventListener(condition = "#event.totalPln >= 1000")
    //   public void flagBigOrder(OrderPlaced event) { ... }
    //
    // Wyrażenie SpEL liczy się PRZED wywołaniem metody; dla zamówień poniżej 1000 zł metoda nie jest wywoływana.
    // W wyrażeniu parametr metody nazywasz #event (nazwa parametru; spring-boot-starter-parent kompiluje z -parameters),
    // a pole rekordu czytasz tak: #event.totalPln (działa — sprawdza to test sekcji 5). Dokumentacja podaje też
    // #a0 i #p0 (numer parametru) oraz #root.event i #root.args.
    //
    // PUŁAPKA: warunek bez znaku #: condition = "event.totalPln >= 1000".
    //   Dlaczego? Bez # Spring szuka pola "event" w obiekcie głównym wyrażenia, a tam "event" to opakowanie
    //   PayloadApplicationEvent, nie rekord. Efekt (test sekcji 8, komunikat z prawdziwego uruchomienia):
    //   SpelEvaluationException: EL1008E: Property or field 'totalPln' cannot be found on object of type
    //   'org.springframework.context.PayloadApplicationEvent' - maybe not public or not valid?
    //   Błąd wychodzi dopiero w chwili PUBLIKACJI, nie przy starcie aplikacji.

    // =================================================================================================
    // 6. SŁUCHACZ ZWRACAJĄCY NOWE ZDARZENIE
    // =================================================================================================
    //
    //   @EventListener PointsGranted grantPoints(OrderPlaced e) { ...; return new PointsGranted(e.customer(), 34); }
    //
    // Jeśli metoda słuchacza zwraca wartość (nie void), Spring publikuje ją jako kolejne zdarzenie (null — nic;
    // kolekcja lub tablica — każdy element osobno). Zdarzenie wynikowe idzie NATYCHMIAST po zakończeniu
    // metody, więc w dzienniku z testu sekcji 6 wpis „punkty-zapisane” pojawia się PRZED „powiadomienie”,
    // choć powiadomienie ma wyższy @Order:
    //   [zamówienie-przyjęte, magazyn, punkty, punkty-zapisane:Jan Kowalski=34, powiadomienie, publikacja-zakończona]
    //
    // DOBRA PRAKTYKA: zdarzenie zwrócone ze słuchacza stosuj oszczędnie. Dlaczego? Łańcuchy zdarzeń
    //   (A wywołuje B, B wywołuje C) trudno śledzić w kodzie i w logach; każde ogniwo działa jeszcze w wątku publikującego.

    // =================================================================================================
    // 7. ZDARZENIE BEZ SŁUCHACZA I SŁUCHACZ SPOZA KONTEKSTU
    // =================================================================================================
    //
    // Test sekcji 7: publishEvent zdarzenia, którego nikt nie czyta — bez błędu i bez śladu. Obiekt utworzony
    // przez new i mający @EventListener nie dostaje niczego: nie jest beanem, więc Spring go nie widzi.
    // Brak @Component = cisza, nie wyjątek.
    //
    // PUŁAPKA: słuchacz bez @Component (albo w pakiecie, którego skanowanie nie obejmuje).
    //   Dlaczego? Spring szuka @EventListener tylko w beanach. Błąd nie daje żadnego komunikatu — zdarzenie
    //   „znika”. Gdy zdarzenie „nie dochodzi”, pierwsze pytanie brzmi: czy ta klasa jest beanem?

    // =================================================================================================
    // 8. PO CO ZDARZENIA, A KIEDY NIE
    // =================================================================================================
    //
    // PO CO: serwis zamówień nie zna magazynu, punktów ani powiadomień — łatwo dodać kolejnego słuchacza i łatwo
    // testować każdego z osobna; moduły (dział s20_modulith) rozmawiają zdarzeniami zamiast wołać się wzajemnie.
    //
    // KIEDY NIE: gdy wywołujący POTRZEBUJE wyniku albo gwarancji kolejności („najpierw rezerwacja, potem płatność”) —
    // wtedy zwykła metoda zwracająca wynik. Zdarzenia ukrywają przepływ sterowania: czytając OrderService, nie widać,
    // co się stanie po publishEvent; kto tego szuka, musi szukać słuchaczy po całym projekcie.
    //
    // PYTANIE REKRUTACYJNE: czym zdarzenia Springa różnią się od kolejki (Kafka, RabbitMQ)? Zdarzenia Springa żyją
    // w jednym procesie i (domyślnie) jednym wątku, nie są trwałe — restart je gubi, nic ich nie ponawia. Kolejka to osobny
    // system, trwały, dla wielu aplikacji (zapowiedź w lekcji Evt03AsyncAndOutbox).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • publisher.publishEvent(rekord) — zdarzenie to zwykły rekord (nie musi dziedziczyć po ApplicationEvent).
     *   • @EventListener na metodzie BEANA; typ pierwszego parametru = typ zdarzenia.
     *   • Domyślnie SYNCHRONICZNIE: ten sam wątek, publishEvent wraca po ostatnim słuchaczu.
     *   • Wyjątek słuchacza leci do publikującego i przerywa pozostałych słuchaczy.
     *   • @Order(n): kolejność (mniejsze = wcześniej); bez niego kolejność nieokreślona.
     *   • condition = "#event.pole >= 1000" (SpEL, ze znakiem #!); zły warunek wychodzi dopiero przy publikacji.
     *   • Wartość zwrócona ze słuchacza (nie void) jest publikowana jako kolejne zdarzenie; null = nic.
     *   • Brak słuchacza lub brak @Component = cisza, nie błąd.
     *   • Nie używaj zdarzeń, gdy potrzebujesz wyniku lub jawnej kolejności — zwykłe wywołanie jest czytelniejsze.
     *
     * PYTANIA KONTROLNE:
     *   1. Co się stanie? Pierwszy z dwóch słuchaczy zdarzenia rzuca wyjątek, a serwis wywołał publishEvent
     *      — czy drugi słuchacz dostanie zdarzenie i czy wyjątek zobaczy serwis?
     *   2. Co się stanie? Publikujesz rekord, na który żaden bean nie nasłuchuje.
     *   3. W którym wątku działa słuchacz, gdy publikujesz zdarzenie z wątku main? Skąd to wiesz?
     *   4. ZNAJDŹ BŁĄD: @EventListener(condition = "event.totalPln >= 1000") — co zobaczysz i kiedy?
     *   5. Co się stanie? Słuchacz o @Order(2) zwraca rekord PointsGranted, a słuchacz o @Order(3) nasłuchuje OrderPlaced
     *      — czy słuchacz PointsGranted wykona się przed czy po słuchaczu o @Order(3)?
     *   6. ZNAJDŹ BŁĄD: klasa z metodą @EventListener nie ma żadnej adnotacji stereotypowej, a zdarzenie „nie dochodzi”.
     *      Dlaczego?
     *   7. Dlaczego zdarzenie nazywamy OrderPlaced, a nie PlaceOrder?
     *   8. Podaj sytuację, w której zdarzenie to zły pomysł, i taką, w której dobry.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Evt01ApplicationEventsExercisesTest (małe konteksty Springa budowane w teście)
    // =================================================================================================

    /** Zdarzenie do ćwiczenia 1: towar prawie się skończył. */
    public record StockLow(String sku, int stock) {
    }

    /**
     * ĆWICZENIE 1 (łatwe): opublikuj zdarzenie {@code new StockLow(sku, stock)} przez podanego publikującego.
     * Podpowiedź: {@code publisher.publishEvent(...)}.
     */
    static void exercise1(ApplicationEventPublisher publisher, String sku, int stock) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): słuchacz zamówień VIP — ma zapisać do listy {@code vipOrderIds} identyfikator
     * zamówienia, ale TYLKO gdy kwota jest co najmniej 1000 zł. Użyj warunku {@code condition} (SpEL),
     * nie instrukcji {@code if} w metodzie. Podpowiedź: {@code @EventListener(condition = "#event.totalPln >= 1000")}.
     */
    public static class Exercise2VipListener {
        public final List<String> vipOrderIds = new ArrayList<>();

        @EventListener
        public void onOrder(OrderPlaced event) {
            // TODO: dodaj condition do adnotacji i zapisz event.orderId() do vipOrderIds
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 3 (średnie): trzej słuchacze tego samego zdarzenia mają zapisać do listy {@code calls} swoje
     * nazwy w kolejności: "pierwszy", "drugi", "trzeci" — niezależnie od kolejności metod w klasie. Użyj {@code @Order}.
     * Podpowiedź: {@code @org.springframework.core.annotation.Order(1)} nad {@code @EventListener}.
     */
    public static class Exercise3OrderedListeners {
        public final List<String> calls = new ArrayList<>();

        @EventListener
        public void third(OrderPlaced event) {
            // TODO: zapisz "trzeci" i ustaw kolejność
            throw new UnsupportedOperationException("TODO");
        }

        @EventListener
        public void first(OrderPlaced event) {
            // TODO: zapisz "pierwszy" i ustaw kolejność
            throw new UnsupportedOperationException("TODO");
        }

        @EventListener
        public void second(OrderPlaced event) {
            // TODO: zapisz "drugi" i ustaw kolejność
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): słuchacz zamówień zwraca {@link PointsGranted} (klient, punkty = pełne dziesiątki
     * złotych z kwoty), a dla zamówień poniżej 100 zł nie zwraca nic ({@code null} — wtedy Spring nic nie publikuje).
     * Podpowiedź: typ zwracany {@code PointsGranted} zamiast {@code void}; {@code totalPln().intValue() / 10}.
     */
    public static class Exercise4PointsListener {
        @EventListener
        public PointsGranted onOrder(OrderPlaced event) {
            // TODO: twoje rozwiązanie
            throw new UnsupportedOperationException("TODO");
        }
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Drugi słuchacz NIE dostanie zdarzenia, a wyjątek wyjdzie z publishEvent do serwisu (kod serwisu po
     *      publishEvent też się nie wykona). Sprawdzone testem (sekcja 3). Słuchacze ustawieni PRZED nieudanym
     *      zdążyliby się wykonać.
     *   2. Nic — brak wyjątku i brak śladu. Sprawdzone testem (sekcja 7).
     *   3. W wątku main (wątku publikującego), bo domyślnie słuchacze są synchroniczni. Dowód: test sekcji 2 porównuje
     *      nazwę wątku zapisaną przez słuchaczy z nazwą wątku testu, a „publikacja-zakończona” jest ostatnim wpisem.
     *   4. SpelEvaluationException: EL1008E: Property or field 'totalPln' cannot be found on object of type
     *      'org.springframework.context.PayloadApplicationEvent' - maybe not public or not valid? — przy PUBLIKACJI
     *      zdarzenia, nie przy starcie. Brakuje znaku # (poprawnie: #event.totalPln). Sprawdzone testem (sekcja 8).
     *   5. Przed. Zdarzenie zwrócone ze słuchacza jest publikowane natychmiast po zakończeniu jego metody. Sprawdzone
     *      testem (sekcja 6): magazyn, punkty, punkty-zapisane, powiadomienie.
     *   6. Spring szuka @EventListener tylko w beanach; klasa bez @Component (lub poza skanowanym pakietem) nie jest
     *      beanem, więc metoda nigdy się nie wywoła i nie ma żadnego komunikatu. Sprawdzone testem (sekcja 7).
     *   7. Zdarzenie opisuje fakt, który już zaszedł (czas przeszły) — słuchacz go nie „wykonuje”, tylko reaguje.
     *      Tryb rozkazujący sugeruje polecenie z jednym wykonawcą i oczekiwaniem wyniku.
     *   8. Zły pomysł: wywołujący potrzebuje wyniku lub ściśle określonej kolejności („najpierw rezerwacja, potem
     *      płatność”). Dobry: kilka niezależnych reakcji na fakt (magazyn, punkty, powiadomienie), które chcesz móc
     *      dodawać bez zmiany serwisu.
     */
    // </editor-fold>
}
