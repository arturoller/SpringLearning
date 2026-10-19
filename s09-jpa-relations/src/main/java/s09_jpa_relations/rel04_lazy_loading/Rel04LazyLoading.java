package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <pre>
 * TEMAT: Leniwe ładowanie od środka — proxy, LazyInitializationException i open-in-view
 *        (lazy loading = leniwe ładowanie; eager = zachłanne; proxy = zastępca; session = sesja;
 *         open-in-view = „otwarty aż do widoku”; fetch = pobieranie)
 *
 * W SKRÓCIE:
 *   Leniwe pole (LAZY) to obietnica: „dociągnę dane z bazy, gdy po nie sięgniesz”. Obietnicę może spełnić tylko
 *   OTWARTA sesja Hibernate (EntityManager). W Springu sesja żyje razem z transakcją — po wyjściu z metody
 *   {@code @Transactional} jest zamykana, a sięgnięcie po niezainicjowane pole kończy się wyjątkiem
 *   LazyInitializationException. Spring Boot domyślnie „ratuje” aplikacje webowe ustawieniem
 *   spring.jpa.open-in-view=true: sesja żyje do końca żądania HTTP. Kosztem jest połączenie z bazą trzymane przez
 *   całe żądanie i zapytania SQL rozsiane po kontrolerach i serializacji JSON. Dobra praktyka: wszystkie relacje
 *   LAZY, open-in-view wyłączone, a potrzebne dane dociągane JAWNIE (join fetch, @EntityGraph) w serwisie.
 *
 * ANALOGIA: wypożyczalnia z magazynem na zapleczu.
 *   Na półce stoi tylko kartonik z numerem płyty (proxy — zna id). Gdy klient chce obejrzeć płytę, sprzedawca
 *   idzie na zaplecze (SELECT). Ale po zamknięciu sklepu (koniec transakcji) zaplecze jest zamknięte na klucz —
 *   z samym kartonikiem w ręku nic nie zrobisz (LazyInitializationException). Open-in-view to zostawienie
 *   otwartego zaplecza i sprzedawcy w pracy do wyjścia ostatniego klienta — wygodne, ale kosztowne.
 *
 * JAK TO DZIAŁA:
 *   orderService.findOrder("ZAM-001")         ← @Transactional(readOnly = true): sesja otwarta
 *     select ... from orders where number=?   ← w polu customer: Customer$HibernateProxy (zna tylko id)
 *                                             ← w polu lines: PersistentBag, niezainicjowany
 *   koniec metody → commit → sesja ZAMKNIĘTA (bez open-in-view)
 *   order.getCustomer().getId()               → 1 (id proxy zna bez bazy)
 *   order.getCustomer().getName()             → LazyInitializationException: Could not initialize proxy
 *                                               [s09_jpa_relations.rel04_lazy_loading.Customer#1] - no session
 *   Z open-in-view: OpenEntityManagerInViewInterceptor (przechwytywacz Springa) otwiera EntityManager na początku
 *   żądania HTTP i zamyka po wysłaniu odpowiedzi — getName() wyśle SELECT w kontrolerze.
 *
 * SŁÓWKA:
 *   lazy = leniwy; eager = zachłanny; proxy = zastępca (podklasa encji generowana przez Hibernate); initialize =
 *   zainicjować (wczytać dane); session = sesja (otwarty EntityManager); no session = brak sesji; interceptor =
 *   przechwytywacz; view rendering = renderowanie widoku (tu: zamiana obiektu na JSON); connection pool = pula
 *   połączeń; unproxy = odpakować z proxy; entity graph = graf encji (lista pól do pobrania od razu).
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel01ManyToOne (LAZY kontra EAGER, proxy), s09_jpa_relations/Rel05NPlusOne
 *             (koszt leniwego ładowania w pętli), s09_jpa_relations/Rel06EntityToDto (DTO zamiast encji w kontrolerze),
 *             s08_data_jpa/Jpa01Entities (kontekst utrwalania).
 * </pre>
 */
public final class Rel04LazyLoading {

    private Rel04LazyLoading() {
    }

    // =================================================================================================
    // 1. LAZY i EAGER — wartości domyślne i dobra praktyka
    // =================================================================================================
    //
    // Wartości domyślne atrybutu fetch w adnotacjach JPA 3.2 (sprawdzone testem — czytamy je z adnotacji):
    //     @ManyToOne   → EAGER          @OneToMany  → LAZY
    //     @OneToOne    → EAGER          @ManyToMany → LAZY
    // Logika specyfikacji: „jeden obiekt jest tani, kolekcja może być ogromna”. W praktyce i jeden obiekt bywa
    // drogi — EAGER działa ZAWSZE, w każdym zapytaniu, także tam, gdzie nikt go nie potrzebuje (Rel01ManyToOne:
    // 3 produkty z EAGER-kategorią = 4 zapytania).
    //
    // DOBRA PRAKTYKA: WSZYSTKIE relacje LAZY: @ManyToOne(fetch = FetchType.LAZY), @OneToOne(fetch = FetchType.LAZY);
    //   kolekcje zostaw domyślne. Dlaczego? EAGER zapisany w encji obowiązuje WSZĘDZIE, gdzie encja jest czytana
    //   (find, JPQL, metody repozytoriów), a LAZY pozwala w każdym zapytaniu osobno zdecydować, co dociągnąć
    //   (sekcja 4). Decyzja „co pobrać” należy do przypadku użycia, nie do encji.

    // =================================================================================================
    // 2. Proxy i leniwa kolekcja od środka
    // =================================================================================================
    //
    // W polu customer wczytanego zamówienia siedzi obiekt klasy Customer$HibernateProxy:
    //   • to PODKLASA Customer wygenerowana w czasie działania (ByteBuddy) → order.getCustomer() instanceof Customer;
    //   • implementuje interfejs org.hibernate.proxy.HibernateProxy;
    //   • zna id (Hibernate przeczytał customer_id z wiersza zamówienia) → getId() NIE idzie do bazy;
    //   • każda inna metoda (getName()) najpierw ją inicjalizuje: select ... from customers where id=?
    //   • Hibernate.unproxy(proxy) — inicjalizuje i zwraca „prawdziwy” obiekt klasy Customer (inny niż proxy).
    //
    // Kolekcja lines to PersistentBag (kolekcja Hibernate). Niezainicjowana nie ma elementów; pierwsze size(),
    // iterator() czy get(0) wczytuje CAŁĄ kolekcję jednym zapytaniem (SQL z logu, z ORDER BY z @OrderBy("id")):
    //     select l1_0.order_id,l1_0.id,l1_0.product_name,l1_0.quantity from order_lines l1_0
    //     where l1_0.order_id=? order by l1_0.id
    //
    // Hibernate.isInitialized(obiekt) mówi, czy proxy/kolekcja ma już dane; Hibernate.initialize(obiekt) wymusza
    // wczytanie (w otwartej sesji).
    //
    // PUŁAPKA: getClass() proxy to NIE Customer.class. Dlaczego to ważne? equals z warunkiem
    //   getClass() != o.getClass() uzna proxy i encję za różne obiekty. Dlatego w equals używamy instanceof
    //   i getId() (Rel01ManyToOne, s08_data_jpa/Jpa01Entities).

    // =================================================================================================
    // 3. LazyInitializationException — leniwe pole po zamknięciu sesji
    // =================================================================================================
    //
    // ShopOrder order = orderService.findOrder("ZAM-001");   // transakcja i sesja już zamknięte
    // order.getCustomer().getId();      → 1 — działa, id jest w proxy
    // order.getCustomer().getName();    → org.hibernate.LazyInitializationException:
    //                                     Could not initialize proxy [s09_jpa_relations.rel04_lazy_loading.Customer#1]
    //                                     - no session
    // order.getLines().size();          → org.hibernate.LazyInitializationException:
    //                                     Cannot lazily initialize collection of role
    //                                     's09_jpa_relations.rel04_lazy_loading.ShopOrder.lines' with key '1' (no session)
    // (komunikaty skopiowane z uruchomienia; „#1” i „key '1'” to id klienta i zamówienia)
    //
    // PYTANIE REKRUTACYJNE: „Skąd się bierze LazyInitializationException i jak go uniknąć?” — sesja zamknięta,
    //   a kod sięga po niezainicjowane leniwe pole. Rozwiązania: sekcja 4 (nie „włącz EAGER”!).
    //
    // PUŁAPKA: Hibernate.getClass(proxy) („jaka to naprawdę klasa?”) INICJALIZUJE proxy — poza sesją rzuca ten sam
    //   LazyInitializationException (sprawdzone testem). Dlaczego to ważne? Wygląda na niewinną metodę
    //   pomocniczą, a idzie do bazy. Klasę bez inicjalizacji poda
    //   ((HibernateProxy) obiekt).getHibernateLazyInitializer().getPersistentClass() (ćwiczenie 3).
    //
    // PUŁAPKA: toString (także generowany przez Lomboka @Data/@ToString) z polami leniwymi. Dlaczego? Logowanie
    //   encji poza transakcją rzuci LazyInitializationException, a w transakcji — wyśle niespodziewane zapytania.
    //   Dlatego toString encji w kursie pokazuje tylko proste pola.

    // =================================================================================================
    // 4. Jak to naprawić: dociągnij JAWNIE, w transakcji
    // =================================================================================================
    //
    // a) join fetch w zapytaniu (ShopOrderRepository.findWithCustomerByNumber) — SQL z logu:
    //        select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.city,c1_0.email,c1_0.name,so1_0.number,so1_0.status
    //        from orders so1_0 join customers c1_0 on c1_0.id=so1_0.customer_id where so1_0.number=?
    //    Jedno zapytanie, w polu customer prawdziwy Customer (nie proxy) — bezpieczny także po transakcji.
    // b) @EntityGraph(attributePaths = "lines") na metodzie repozytorium — Spring Data przekazuje Hibernate graf
    //    „pobierz też lines”; SQL z logu: ... from orders so1_0 left join order_lines l1_0 on so1_0.id=l1_0.order_id
    //    where so1_0.number=? order by l1_0.id. Uwaga: graf dał LEFT JOIN (zamówienie bez pozycji też wróci),
    //    a „join fetch” z punktu a — zwykły JOIN; jeśli relacja może być pusta, pisz „left join fetch”.
    // c) użycie leniwego pola WEWNĄTRZ metody @Transactional (OrderService.customerNameOf) — sesja jest otwarta,
    //    więc działa (kosztem drugiego SELECT-a).
    // d) najlepiej: w transakcji zbuduj DTO z potrzebnymi polami i to jego oddaj na zewnątrz (Rel06EntityToDto).
    //
    // PUŁAPKA: Hibernate.isInitialized i Hibernate.initialize nie widzą kolekcji schowanej w opakowaniu
    //   Collections.unmodifiableList (wzorzec z Rel02OneToMany). Dla opakowania isInitialized zwraca ZAWSZE true
    //   (to „zwykły obiekt”, nie kolekcja Hibernate), a initialize nic nie robi. Dlaczego to ważne? Test
    //   „czy pozycje są wczytane” przechodzi, a w produkcji leci LazyInitializationException. Inicjalizuj przez
    //   size() albo zapytaniem (join fetch / @EntityGraph).
    //
    // PUŁAPKA: spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true („leniwe ładowanie bez transakcji”).
    //   Wyjątek znika, bo każde leniwe pole poza transakcją otwiera WŁASNĄ, chwilową sesję i wysyła osobne
    //   zapytanie (test: 2 dodatkowe SELECT-y dla klienta i pozycji). Dlaczego to zły pomysł? Ukrywa problem
    //   zamiast go rozwiązać: zapytania poza transakcją, każde z osobnym połączeniem, łatwo o N+1 (Rel05NPlusOne).

    // =================================================================================================
    // 5. spring.jpa.open-in-view — domyślnie WŁĄCZONE (aplikacje webowe)
    // =================================================================================================
    //
    // W aplikacji webowej (Spring MVC + JPA) Boot rejestruje OpenEntityManagerInViewInterceptor
    // (autokonfiguracja JpaBaseConfiguration.JpaWebConfiguration, warunek spring.jpa.open-in-view=true albo BRAK
    // ustawienia). Jeśli nie ustawisz tej właściwości, przy starcie zobaczysz w logu (poziom WARN):
    //     spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during
    //     view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
    // („open-in-view jest domyślnie włączone. Dlatego zapytania do bazy mogą być wykonywane podczas renderowania
    // widoku. Ustaw jawnie spring.jpa.open-in-view, żeby wyłączyć to ostrzeżenie”).
    //
    // OrderController.order robi to, czego robić nie należy: bierze encję z serwisu i PO transakcji czyta
    // order.getCustomer().getName(). Wyniki (testy MockMvc):
    //   open-in-view włączone → 200, {"number":"ZAM-001","customerName":"Jan Kowalski","busyConnections":1}
    //                           (SELECT klienta poszedł z kontrolera, poza transakcją)
    //   open-in-view=false    → ServletException „Request processing failed: org.hibernate.LazyInitializationException:
    //                           Could not initialize proxy [...Customer#1] - no session” (na prawdziwym serwerze: 500)
    //
    // Druga, mniej widoczna różnica — endpoint /status w ogóle nie dotyka leniwych pól, a mimo to:
    //   open-in-view włączone → busyConnections = 1: połączenie z bazy jest zajęte do KOŃCA żądania;
    //   open-in-view=false    → busyConnections = 0: połączenie wróciło do puli zaraz po transakcji serwisu.
    // Dlaczego? Spring konfiguruje Hibernate z hibernate.connection.handling_mode = DELAYED_ACQUISITION_AND_HOLD
    // (HibernateJpaVendorAdapter): sesja pobiera połączenie przy pierwszym zapytaniu i TRZYMA je, dopóki sama
    // żyje. Z open-in-view sesja żyje do końca żądania.
    //
    // DOBRA PRAKTYKA: w każdej aplikacji webowej z JPA ustaw spring.jpa.open-in-view=false. Dlaczego?
    //   • połączenie trzymane przez całe żądanie (także w czasie wolnej serializacji, wywołań innych serwisów)
    //     → przy większym ruchu pula połączeń (domyślnie 10 w HikariCP) się wyczerpuje;
    //   • zapytania SQL wykonywane poza transakcją, w kontrolerze i w czasie zamiany obiektu na JSON — trudne do
    //     zauważenia i do zmierzenia, łatwo o N+1;
    //   • wyłączone ujawnia błędy (LazyInitializationException) od razu, w testach, a nie dopiero pod obciążeniem.
    // Lekcja Rel06EntityToDto ma open-in-view=false i pokazuje poprawny wzorzec: serwis w transakcji buduje DTO.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Domyślnie: @ManyToOne/@OneToOne EAGER, @OneToMany/@ManyToMany LAZY. Ustawiaj wszystko na LAZY.
     *   • Proxy: Klasa$HibernateProxy, instanceof Klasa, getId() bez bazy, inne metody → SELECT.
     *   • Hibernate.isInitialized / initialize / unproxy; Hibernate.getClass(proxy) INICJALIZUJE.
     *   • Po zamknięciu sesji: „Could not initialize proxy [...] - no session” albo
     *     „Cannot lazily initialize collection of role '...' with key '...' (no session)”.
     *   • Naprawa: join fetch / left join fetch, @EntityGraph, użycie w transakcji, DTO budowane w serwisie.
     *   • NIE: EAGER, enable_lazy_load_no_trans, open-in-view jako „lekarstwo”.
     *   • spring.jpa.open-in-view domyślnie true (+ WARN w logu); ustaw false — połączenie wraca do puli po transakcji.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie są domyślne wartości fetch dla @ManyToOne, @OneToOne, @OneToMany i @ManyToMany?
     *   2. Co się stanie? Serwis z @Transactional zwraca zamówienie z findByNumber. Poza serwisem wołasz
     *      order.getCustomer().getId(), a potem order.getCustomer().getName().
     *   3. ZNAJDŹ BŁĄD: test sprawdza {@code assertThat(Hibernate.isInitialized(order.getLines())).isTrue()}, a getter
     *      zwraca {@code Collections.unmodifiableList(lines)}. Test przechodzi, ale w produkcji leci
     *      LazyInitializationException. Dlaczego?
     *   4. Czym różni się SQL wygenerowany przez „join fetch o.customer” od @EntityGraph(attributePaths = "lines")?
     *   5. Co się stanie? W aplikacji ze spring.jpa.open-in-view=false kontroler bierze encję z serwisu i czyta
     *      leniwe pole klienta.
     *   6. Dlaczego przy włączonym open-in-view połączenie z bazą jest zajęte do końca żądania, nawet gdy kontroler
     *      nie dotyka żadnego leniwego pola?
     *   7. Dlaczego hibernate.enable_lazy_load_no_trans=true to zły sposób na LazyInitializationException?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rel04LazyLoadingExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwę klienta zamówienia o podanym numerze. Test woła tę metodę POZA transakcją,
     * a findByNumber zwraca zamówienie z leniwym klientem. Podpowiedź: wykonaj całą pracę w
     * {@code tx.execute(status -> ...)} — TransactionTemplate (szablon transakcji) otwiera transakcję na czas lambdy.
     */
    static String exercise1(TransactionTemplate tx, ShopOrderRepository orders, String number) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): jednym zapytaniem JPQL wczytaj zamówienie o podanym numerze RAZEM z klientem
     * i pozycjami — tak, żeby po zakończeniu transakcji oba pola były zainicjowane. Test liczy zapytania: ma być
     * dokładnie 1. Podpowiedź: {@code select o from ShopOrder o join fetch o.customer left join fetch o.lines
     * where o.number = :number}.
     */
    static ShopOrder exercise2(EntityManager em, String number) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć prawdziwą klasę encji (np. Customer.class) dla obiektu, który może być proxy
     * — BEZ inicjalizowania proxy (test woła metodę poza sesją!). Dla zwykłego obiektu zwróć getClass().
     * Podpowiedź: {@code if (entity instanceof HibernateProxy proxy)} →
     * {@code proxy.getHibernateLazyInitializer().getPersistentClass()}. Hibernate.getClass tu NIE zadziała — dlaczego?
     */
    static Class<?> exercise3(Object entity) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @ManyToOne i @OneToOne — EAGER; @OneToMany i @ManyToMany — LAZY (wartości domyślne w adnotacjach JPA 3.2,
     *      sprawdzone testem defaultFetchTypes).
     *   2. getId() zwraca id (proxy zna je bez bazy). getName() rzuca LazyInitializationException „Could not initialize
     *      proxy [s09_jpa_relations.rel04_lazy_loading.Customer#1] - no session” — sesja zamknęła się razem z
     *      transakcją serwisu. Sprawdzone testami proxyIdWorksOutsideSession i proxyOutsideSessionThrows.
     *   3. Opakowanie z unmodifiableList to nie jest kolekcja Hibernate, więc Hibernate.isInitialized traktuje je jak
     *      zwykły obiekt i zwraca true — choć ukryty w środku PersistentBag nie jest wczytany. Sprawdzone testem
     *      unmodifiableWrapperFoolsIsInitialized.
     *   4. join fetch o.customer → zwykły JOIN (inner join) z customers; @EntityGraph → LEFT JOIN z order_lines.
     *      Oba wczytują dane w tym samym SELECT. (SQL z logu w sekcji 4.)
     *   5. LazyInitializationException — w MockMvc jako przyczyna ServletException „Request processing failed”,
     *      na prawdziwym serwerze odpowiedź 500. Sprawdzone testem Rel04OpenInViewOffTest.lazyFieldInControllerFails.
     *   6. Open-in-view otwiera EntityManager na całe żądanie, a Spring ustawia Hibernate tryb
     *      DELAYED_ACQUISITION_AND_HOLD: połączenie pobrane przy pierwszym zapytaniu jest trzymane, dopóki sesja żyje.
     *      Test: busyConnections = 1 z open-in-view, 0 bez niego.
     *   7. Ukrywa błąd: każde leniwe pole poza transakcją otwiera osobną sesję i wysyła osobne zapytanie (poza
     *      transakcją, łatwo o N+1). Sprawdzone testem Rel04LazyLoadNoTransTest.
     */
    // </editor-fold>
}
