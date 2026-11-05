package s09_jpa_relations.rel04_lazy_loading;

/**
 * <pre>
 * TEMAT: Leniwe ładowanie (lazy loading) — LAZY kontra EAGER, proxy Hibernate, {@code LazyInitializationException}
 *        i sposoby na dociągnięcie danych, zanim zamknie się transakcja
 *        (lazy = leniwy; eager = zachłanny; fetch type = typ pobierania; proxy = pośrednik, zastępca;
 *         lazy initialization = leniwa inicjalizacja)
 *
 * W SKRÓCIE:
 *   Relacja LAZY nie jest czytana z bazy razem z encją. W jej polu siedzi „zastępca”: proxy (dla relacji „do jednego”)
 *   albo kolekcja Hibernate (dla „do wielu”), które same wyślą SELECT przy pierwszym użyciu. Działa to TYLKO, dopóki
 *   sesja Hibernate (EntityManager) jest otwarta — w praktyce: dopóki trwa transakcja. Po jej końcu dostęp do
 *   niewczytanej relacji kończy się {@code LazyInitializationException}. Rozwiązanie NIE polega na EAGER, tylko na
 *   świadomym wczytaniu tego, czego potrzebujesz, w transakcji: serwis z {@code @Transactional}, {@code join fetch},
 *   {@code @EntityGraph} albo projekcja do DTO.
 *
 * ANALOGIA: bilet do szatni.
 *   Oddajesz płaszcz i dostajesz numerek (proxy). Numerek zna numer wieszaka (id) — możesz go odczytać od razu, bez
 *   szatniarza (getId() bez SQL). Ale po płaszcz (nazwisko klienta, pozycje zamówienia) musi pójść szatniarz (sesja
 *   Hibernate). Gdy teatr zamknięto (koniec transakcji), z samym numerkiem płaszcza nie odbierzesz —
 *   to jest LazyInitializationException. Rada: odbierz płaszcz, zanim wyjdziesz z teatru.
 *
 * JAK TO DZIAŁA:
 *   1. {@code find} zamówienia czyta tylko tabelę orders. W pole {@code customer} Hibernate wstawia obiekt klasy
 *      {@code Customer$HibernateProxy…} (podklasa Customer wygenerowana w czasie działania) z samym id z kolumny
 *      customer_id, a w pole {@code lines} — {@code PersistentBag} z flagą „niezainicjalizowany”.
 *   2. Pierwsze wywołanie metody innej niż getId() na proxy (albo size(), iteracja na kolekcji) prosi sesję o
 *      SELECT. Sesja jest związana z transakcją Springa (EntityManager współdzielony — shared EntityManager).
 *   3. Repozytorium Spring Data ma własną transakcję na KAŻDE wywołanie metody ({@code SimpleJpaRepository} jest
 *      {@code @Transactional(readOnly = true)}). Wywołane spoza transakcji: otwiera sesję, czyta, ZAMYKA sesję.
 *      Zwrócona encja jest odłączona (detached), a jej proxy nie ma już kogo poprosić o dane → wyjątek.
 *   4. Jeżeli wołający ma własną transakcję (serwis z {@code @Transactional}), repozytorium dołącza się do niej —
 *      ta sama sesja żyje do końca metody serwisu i leniwe relacje się dociągają.
 *
 * SŁÓWKA:
 *   lazy = leniwy; eager = zachłanny; proxy = pośrednik; initialize = zainicjalizować (wczytać);
 *   session = sesja (Hibernate: kontekst utrwalania + połączenie); detached = odłączony; persistent collection =
 *   kolekcja Hibernate śledząca zmiany; bag = worek (lista bez kolejności); entity graph = graf encji (lista relacji
 *   do wczytania); projection = projekcja (tylko wybrane kolumny); read only = tylko do odczytu;
 *   Open Session in View = otwarta sesja aż do widoku (wzorzec webowy).
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel01ManyToOne (EAGER kontra LAZY przy @ManyToOne, proxy i getId()),
 *             s09_jpa_relations/Rel02OneToMany (leniwa lista z mappedBy),
 *             s09_jpa_relations/Rel05NPlusOne (join fetch, @EntityGraph i batch fetching dla LIST encji),
 *             s09_jpa_relations/Rel06EntityToDto (DTO zamiast encji na zewnątrz),
 *             s08_data_jpa/Jpa01Entities (kontekst utrwalania, stany encji: managed/detached),
 *             s08_data_jpa/Jpa04CustomQueries (@Query, projekcje).
 * </pre>
 */
public final class Rel04LazyLoading {

    private Rel04LazyLoading() {
    }

    // =================================================================================================
    // 1. DOMYŚLNE TYPY POBIERANIA, proxy i kolekcje Hibernate (ShopOrder, Customer)
    // =================================================================================================
    //
    //   Adnotacja         domyślny fetch (JPA)    co siedzi w polu po wczytaniu encji (gdy LAZY)
    //   {@code @ManyToOne}        EAGER                   proxy {@code Customer$HibernateProxy…} (zna tylko id)
    //   {@code @OneToOne}         EAGER                   proxy (strona z kluczem obcym)
    //   {@code @OneToMany}        LAZY                    {@code PersistentBag} (List) / {@code PersistentSet} (Set)
    //   {@code @ManyToMany}       LAZY                    jak wyżej
    //   (domyślne wartości sprawdzone testem — odczytane z samych adnotacji przez refleksję)
    //
    // Reguła łatwa do zapamiętania: „do jednego” = EAGER, „do wielu” = LAZY. Dlatego przy {@code @ManyToOne} piszemy
    // {@code fetch = FetchType.LAZY} JAWNIE (Rel01, sekcja 2 — DOBRA PRAKTYKA), a przy kolekcjach nie trzeba.
    //
    // Co widać w teście (zamówienie 1, sprawdzone z SqlCapture):
    //   {@code orders.findById(1L)}    → {@code select so1_0.id,so1_0.customer_id,so1_0.order_number from orders so1_0 where so1_0.id=?}
    //   {@code order.getCustomer().getClass()}  → {@code s09_jpa_relations.rel04_lazy_loading.Customer$HibernateProxy…}
    //   {@code Hibernate.isInitialized(customer)} → false; {@code customer instanceof Customer} → true (proxy = podklasa)
    //   {@code customer.getId()}       → 1, BEZ SQL (id było w kolumnie customer_id)
    //   {@code customer.getName()}     → {@code select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?}
    //   {@code order.getLines().getClass()} → PersistentBag; {@code size()} → SELECT pozycji
    //   {@code customer.getOrders().getClass()} → PersistentSet
    //   {@code Hibernate.getClass(customer)} → Customer.class (prawdziwa klasa encji za proxy)
    //
    // PUŁAPKA: {@code order.getCustomer().getClass() == Customer.class} jest FAŁSZEM. Dlaczego groźne? equals pisane
    //   z {@code getClass() != o.getClass()} (generowane przez IDE) uzna proxy i encję za RÓŻNE obiekty. Dlatego
    //   encje w kursie mają equals z {@code instanceof} i porównują {@code getId()} (getter działa także na proxy —
    //   pole proxy jest puste!). Szczegóły: s08_data_jpa/Jpa01Entities, sekcja 5.

    // =================================================================================================
    // 2. LazyInitializationException poza transakcją — i dlaczego @DataJpaTest go NIE pokazuje
    // =================================================================================================
    //
    //   PRZED (typowy kod kontrolera albo zadania w tle — BEZ transakcji):
    //     {@code ShopOrder order = orders.findById(1L).orElseThrow();}   // transakcja repozytorium już się skończyła
    //     {@code order.getLines().size();}                               // ← wybuch
    //
    //   Sprawdzone testem na pełnej aplikacji (Rel04OutsideTransactionTest):
    //     lista:  {@code org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role}
    //             {@code 's09_jpa_relations.rel04_lazy_loading.ShopOrder.lines' with key '1' (no session)}
    //     proxy:  {@code order.getCustomer().getName()} → LazyInitializationException
    //             {@code Could not initialize proxy [s09_jpa_relations.rel04_lazy_loading.Customer#1] - no session}
    //     {@code order.getCustomer().getId()} → 1 (bez wyjątku: id proxy zna bez bazy)
    //   W starszych wersjach Hibernate komunikat brzmiał „failed to lazily initialize a collection of role: …,
    //   could not initialize proxy - no Session” — tak go znajdziesz w internecie.
    //
    // PUŁAPKA: ten sam kod w {@code @DataJpaTest} PRZECHODZI (test section2DataJpaTestHidesLazyInitializationException).
    //   Dlaczego? Wycinek {@code @DataJpaTest} jest {@code @Transactional}: cały test to jedna transakcja (wycofywana na
    //   końcu), więc sesja jest otwarta przez cały test i lista „po wyjściu z repozytorium” się dociąga. Zielony test,
    //   czerwona produkcja. To samo dotyczy {@code @SpringBootTest} z {@code @Transactional} na klasie testu.
    //
    // DOBRA PRAKTYKA: kod, który ma działać poza transakcją, testuj BEZ transakcji testu:
    //   {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} (propagation = propagacja, not supported = „nie
    //   obsługuj transakcji”) na metodzie albo klasie testu — wtedy wycinek pokazuje prawdę: LazyInitializationException
    //   (sprawdzone testem). Uwaga: bez transakcji testu zmiany w bazie są ZATWIERDZANE — w takich testach tylko czytaj
    //   albo sprzątaj po sobie. Tak działają testy ćwiczeń tej lekcji.

    // =================================================================================================
    // 3. ROZWIĄZANIA — wczytaj potrzebne dane, ZANIM transakcja się skończy
    // =================================================================================================
    //
    // a) Transakcja w serwisie: {@code @Transactional(readOnly = true)} (OrderReadService.describe). Wszystkie leniwe
    //    relacje czytasz w metodzie serwisu i zwracasz gotowy wynik (String, DTO). Dla zamówienia 1 poszło 5 zapytań
    //    (zamówienie, pozycje, 2 produkty, klient — sprawdzone testem) — działa, ale przy liście zamówień to N+1
    //    (s09_jpa_relations/Rel05NPlusOne).
    //
    // b) {@code join fetch} w JPQL (ShopOrderRepository.findWithDetailsById) — JEDNO zapytanie z JOIN-ami do customer,
    //    order_line i product (SQL w komentarzu repozytorium, sprawdzony testem). Encja wraca z relacjami wczytanymi —
    //    działa poza transakcją. Uwaga: {@code join fetch} to złączenie WEWNĘTRZNE — zamówienie bez pozycji nie
    //    zostałoby znalezione; wtedy {@code left join fetch} (Rel05).
    //
    // c) {@code @EntityGraph(attributePaths = "customer")} (graf encji) na metodzie repozytorium — to samo bez pisania
    //    JPQL: Spring Data buduje zapytanie z nazwy metody, a graf dokłada JOIN do klienta. Graf obejmuje TYLKO
    //    wymienione relacje — lista pozycji z findWithCustomerByOrderNumber poza transakcją nadal rzuca wyjątek (test).
    //
    // d) Projekcja / DTO: {@code select new …OrderView(o.orderNumber, o.customer.name)} — żadnej encji, żadnego proxy,
    //    1 zapytanie, 0 wczytanych encji (test). Najlepsze do odczytu „na zewnątrz” (s09_jpa_relations/Rel06EntityToDto).
    //
    // ANTYWZORZEC: zmiana relacji na EAGER „żeby działało”. Dlaczego zły? EAGER działa ZAWSZE — także tam, gdzie relacji
    //   nikt nie potrzebuje — i każde zapytanie JPQL o listę zamienia w N+1 (Rel01, sekcja 2). Nie da się go wyłączyć
    //   w konkretnym zapytaniu, a LAZY da się „włączyć” (b, c).
    //
    // ANTYWZORZEC: {@code spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true}. Hibernate pozwala wtedy
    //   inicjalizować proxy BEZ sesji: na każde dociągnięcie otwiera NOWĄ, tymczasową sesję (i bierze połączenie z puli).
    //   Sprawdzone testem: zamówienie z 2 pozycjami poza transakcją → 3 zapytania i 3 otwarte sesje (pozycje + każdy
    //   produkt osobno). Dlaczego zły? Ukrywa N+1, każde dociągnięcie to osobna „transakcja” (brak spójnego obrazu
    //   danych), a błąd projektowy (brak granicy transakcji) zostaje — tylko przestaje być widoczny.
    //
    // DOBRA PRAKTYKA: granicą transakcji jest metoda SERWISU; serwis zwraca DTO (albo encję z jawnie wczytanymi
    //   relacjami — b, c), a relacje wczytuje świadomie zapytaniem. Dlaczego? Wiesz, ile zapytań idzie do bazy, i nic
    //   nie wybucha w warstwie, która o bazie nie powinna nic wiedzieć.
    //   PYTANIE REKRUTACYJNE: „Skąd się bierze LazyInitializationException i jak go poprawnie rozwiązać?”

    // =================================================================================================
    // 4. Open Session in View (spring.jpa.open-in-view) — sesja otwarta aż do końca żądania HTTP
    // =================================================================================================
    //
    // W aplikacji WEBOWEJ (Spring MVC) Boot domyślnie rejestruje {@code OpenEntityManagerInViewInterceptor}
    // (interceptor = przechwytywacz żądań): otwiera EntityManager na początku żądania HTTP i zamyka go po wysłaniu
    // odpowiedzi. Leniwe relacje dociągają się wtedy nawet w kontrolerze i przy zamianie na JSON — wyjątku nie ma.
    // Ponieważ to ustawienie jest domyślnie WŁĄCZONE, Boot wypisuje przy starcie ostrzeżenie:
    // „spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view
    // rendering. Explicitly configure spring.jpa.open-in-view to disable this warning”.
    //
    // A bez weba (ten moduł)? Sprawdzone testem: konfiguracja OSIV w Boot 4 (JpaBaseConfiguration.JpaWebConfiguration)
    // działa tylko w aplikacji typu SERVLET z klasami Spring MVC. Tu ich nie ma, więc {@code spring.jpa.open-in-view=true}
    // NICZEGO nie zmienia: brak beana openEntityManagerInViewInterceptor, a LazyInitializationException jest dalej.
    // Zadania w tle, konsumenci kolejek, {@code @Scheduled} nigdy nie mają OSIV — tam granicą jest tylko transakcja.
    //
    // DOBRA PRAKTYKA: {@code spring.jpa.open-in-view=false} w każdej aplikacji webowej (w tej lekcji zapisane jawnie
    //   w application-rel04.properties). Dlaczego? (1) Połączenie z bazą jest trzymane przez CAŁE żądanie — także
    //   podczas wolnego wywołania innej usługi i serializacji; pula połączeń wyczerpuje się pod obciążeniem.
    //   (2) Zapytania „rozlewają się” po kontrolerach i widokach — N+1 robi się niewidoczny. (3) Kod działa w webie,
    //   a ten sam serwis wywołany z zadania w tle rzuca LazyInitializationException.
    //   PYTANIE REKRUTACYJNE: „Co to jest Open Session in View i dlaczego wielu zespołów go wyłącza?”

    // =================================================================================================
    // 5. getReferenceById a proxy; Hibernate.initialize — kiedy
    // =================================================================================================
    //
    // {@code getReferenceById(id)} (Spring Data; pod spodem {@code EntityManager.getReference}) zwraca proxy BEZ
    // zapytania (test: 0 SQL). Idealne, gdy potrzebujesz tylko „wskaźnika” do ustawienia relacji (Rel01, sekcja 3).
    //   • {@code getId()} — bez SQL; pierwszy inny getter w transakcji — SELECT klienta;
    //   • nieistniejące id (99) — {@code getReferenceById} NIE rzuca; wyjątek {@code jakarta.persistence.EntityNotFoundException}
    //     pojawia się dopiero przy pierwszym getterze (sprawdzone testem);
    //   • poza transakcją — proxy zwrócone, ale getter rzuca LazyInitializationException (sprawdzone testem).
    //
    // {@code Hibernate.initialize(obiekt)} — „wczytaj teraz” proxy albo kolekcję; {@code Hibernate.isInitialized(obiekt)} —
    // „czy już wczytane?” (nie wysyła SQL). Kiedy initialize? Gdy w transakcji serwisu chcesz oddać ENCJĘ z konkretną
    // relacją gotową do użycia, a nie chcesz pisać osobnego zapytania (OrderReadService.findWithLinesInitialized).
    //
    // PUŁAPKA: initialize wczytuje TYLKO wskazany obiekt, nie relacje jego elementów. Sprawdzone testem: po
    //   {@code Hibernate.initialize(order.getLines())} lista działa poza transakcją, ale
    //   {@code line.getProduct().getName()} rzuca LazyInitializationException — produkty nadal są proxy. Dlaczego ważne?
    //   Każdy kolejny poziom to kolejne initialize i kolejne zapytania (N+1!). Gdy potrzebujesz kilku poziomów —
    //   {@code join fetch} albo @EntityGraph (sekcja 3).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Domyślnie: @ManyToOne/@OneToOne EAGER, @OneToMany/@ManyToMany LAZY → przy „do jednego” pisz LAZY jawnie.
     *   • LAZY „do jednego” = proxy {@code Klasa$HibernateProxy…} (getId() bez SQL); LAZY „do wielu” = PersistentBag/Set.
     *   • {@code Hibernate.isInitialized(x)} — sprawdź bez SQL; {@code Hibernate.initialize(x)} — wczytaj teraz (1 poziom).
     *   • Poza transakcją: LazyInitializationException „Cannot lazily initialize collection … (no session)” /
     *     „Could not initialize proxy […#1] - no session”.
     *   • @DataJpaTest (i @Transactional na teście) UKRYWA ten wyjątek; pokaże go
     *     {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} albo test pełnej aplikacji.
     *   • Rozwiązania: @Transactional(readOnly = true) w serwisie, {@code join fetch}, @EntityGraph, projekcja/DTO.
     *   • Nie: EAGER „żeby działało”, {@code hibernate.enable_lazy_load_no_trans=true} (nowa sesja na każde proxy).
     *   • spring.jpa.open-in-view: domyślnie true tylko w webie (ostrzeżenie w logu); bez weba bez znaczenia;
     *     DOBRA PRAKTYKA: false.
     *   • getReferenceById: proxy bez SQL; brak wiersza → EntityNotFoundException dopiero przy getterze.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki jest domyślny typ pobierania dla @ManyToOne, @OneToOne, @OneToMany i @ManyToMany?
     *   2. Co się stanie? W metodzie BEZ transakcji: {@code ShopOrder o = orders.findById(1L).orElseThrow();}
     *      {@code System.out.println(o.getCustomer().getId()); System.out.println(o.getLines().size());}
     *   3. ZNAJDŹ BŁĄD: test {@code @DataJpaTest} sprawdza {@code orders.findById(1L).get().getLines()} i przechodzi,
     *      a ten sam kod w kontrolerze rzuca wyjątek. Co jest nie tak z testem i jak go poprawić?
     *   4. Dlaczego {@code @ManyToOne(fetch = EAGER)} albo {@code enable_lazy_load_no_trans} to złe „naprawy”
     *      LazyInitializationException?
     *   5. Co się stanie? Serwis zwraca zamówienie po {@code Hibernate.initialize(order.getLines())}, a wywołujący
     *      poza transakcją czyta {@code order.getLines().get(0).getProduct().getName()}.
     *   6. Co robi {@code spring.jpa.open-in-view=true} w aplikacji webowej, a co w aplikacji bez weba? Dlaczego warto
     *      ustawić false?
     *   7. Co się stanie? {@code Customer c = customers.getReferenceById(99L);} (klienta 99 nie ma), a potem
     *      w tej samej transakcji {@code c.getName()}.
     *   8. Czym różni się {@code join fetch} od @EntityGraph i od projekcji {@code select new}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku LazyLoadingExercises.java (metody z TODO), testy: Rel04LazyLoadingExercisesTest
    // (działają BEZ transakcji testu). Potrzebne zapytania dopisujesz w ExerciseShopOrderRepository.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @ManyToOne i @OneToOne — EAGER; @OneToMany i @ManyToMany — LAZY (zgodnie ze specyfikacją JPA; sprawdzone
     *      testem na samych adnotacjach). Dlatego przy relacjach „do jednego” ustawiamy LAZY ręcznie.
     *   2. Pierwsza linia wypisze 1 (proxy zna id z kolumny customer_id, bez SQL). Druga rzuci
     *      LazyInitializationException „Cannot lazily initialize collection of role '…ShopOrder.lines' with key '1'
     *      (no session)” — transakcja repozytorium skończyła się w findById. Sprawdzone testem pełnej aplikacji.
     *   3. @DataJpaTest uruchamia każdy test w transakcji, więc sesja jest otwarta i lista się dociąga — test nie
     *      odwzorowuje produkcji. Poprawka: {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} na teście
     *      (wtedy test pokaże wyjątek) albo test pełnej aplikacji; a w kodzie — transakcja serwisu / join fetch / DTO.
     *   4. EAGER wczytuje relację zawsze, także gdy jej nie potrzeba, i robi N+1 przy każdym zapytaniu JPQL o listę;
     *      nie da się go wyłączyć dla jednego zapytania. enable_lazy_load_no_trans otwiera nową sesję na każde proxy
     *      (test: 3 zapytania, 3 sesje dla zamówienia z 2 pozycjami) — ukrywa N+1 i brak granicy transakcji.
     *   5. LazyInitializationException: initialize wczytał tylko listę pozycji, a produkty w pozycjach to nadal
     *      niezainicjalizowane proxy (getId() by zadziałało). Sprawdzone testem. Naprawa: join fetch o.lines l
     *      join fetch l.product albo DTO.
     *   6. W webie (servlet + Spring MVC) rejestruje OpenEntityManagerInViewInterceptor: sesja żyje przez całe żądanie,
     *      więc leniwe relacje dociągają się nawet w kontrolerze i widoku (domyślnie włączone, z ostrzeżeniem w logu).
     *      Bez weba nie robi nic (test: brak beana interceptora, wyjątek dalej jest). false — bo trzyma połączenie przez
     *      całe żądanie, ukrywa N+1 i daje kod działający tylko w webie.
     *   7. Sam getReferenceById nie rzuci i nie wyśle SQL. c.getName() spróbuje wczytać proxy i rzuci
     *      jakarta.persistence.EntityNotFoundException (z nazwą encji Customer i id 99). Sprawdzone testem.
     *   8. join fetch — piszesz JPQL i sam wybierasz relacje oraz rodzaj złączenia; @EntityGraph — dokładasz listę
     *      relacji do metody repozytorium (także z nazwy), bez JPQL; oba zwracają ENCJE z wczytanymi relacjami.
     *      Projekcja select new zwraca rekord/DTO z wybranymi kolumnami — bez encji, bez proxy, bez śledzenia zmian.
     */
    // </editor-fold>
}
